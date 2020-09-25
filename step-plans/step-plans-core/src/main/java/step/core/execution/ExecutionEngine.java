/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.LayeredPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunner;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.Plugin;
import step.core.plugins.PluginManager;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;
import step.engine.plugins.ExecutionEnginePlugin;

/**
 * This class represent the central component for the execution of {@link Plan}s.
 * It supports local executions as well as central executions within controller instances.
 * 
 * It replaces all the classes implementing the legacy {@link PlanRunner} interface
 *
 */
public class ExecutionEngine {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionEngine.class);
	
	private final ExecutionEngineContext executionEngineContext;
	private final ExecutionEnginePlugin plugins;
	private final ConcurrentHashMap<String, ExecutionContext> currentExecutions = new ConcurrentHashMap<>();
	
	private ExecutionEngine(ExecutionEngineContext executionEngineContext, ExecutionEnginePlugin plugins) {
		super();
		
		this.executionEngineContext = executionEngineContext;
		this.plugins = plugins;
	}
	
	public static class Builder {

		private final step.core.plugins.PluginManager.Builder<ExecutionEnginePlugin> pluginBuilder;
		private OperationMode operationMode;
		private AbstractExecutionEngineContext parentContext;
		
		public Builder() {
			super();
			pluginBuilder = PluginManager.builder(ExecutionEnginePlugin.class);
		}
		
		/**
		 * Add a {@link Plugin} instance to the list of {@link Plugin}s to be used by the {@link ExecutionEngine}
		 * @param plugin the instance of {@link Plugin} to be added 
		 * @return 
		 */
		public Builder withPlugin(ExecutionEnginePlugin plugin) {
			pluginBuilder.withPlugin(plugin);
			return this;
		}
		
		/**
		 * Add a {@link List} of {@link Plugin} instances to the list of {@link Plugin}s to be used by the {@link ExecutionEngine}
		 * @param plugins the {@link List} of {@link Plugin} isntances
		 * @return 
		 */
		public Builder withPlugins(List<ExecutionEnginePlugin> plugins) {
			pluginBuilder.withPlugins(plugins);
			return this;
		}
	
		/**
		 * Searches for classes annotated by {@link Plugin} and implementing {@link ExecutionEnginePlugin} in the classpath
		 * and add them to the list of plugins to be used by the {@link ExecutionEngine} 
		 * @return
		 */
		public Builder withPluginsFromClasspath() {
			try {
				pluginBuilder.withPluginsFromClasspath();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				throw new ExecutionEngineException(e);
			}
			return this;
		}
		
		/**
		 * Sets the operation mode of the {@link ExecutionEngine}. {@link OperationMode}
		 * @param operationMode the operation mode of the {@link ExecutionEngine}
		 * @return
		 */
		public Builder withOperationMode(OperationMode operationMode) {
			this.operationMode = operationMode;
			return this;
		}
		
		/**
		 * Sets a parent context from which the attributes should be inherited
		 * @param parentContext
		 * @return
		 */
		public Builder withParentContext(AbstractExecutionEngineContext parentContext) {
			this.parentContext = parentContext;
			return this;
		}
		
		/**
		 * @return creates the {@link ExecutionEngine} instance
		 */
		public ExecutionEngine build() {
			if(operationMode == null) {
				operationMode = OperationMode.LOCAL;
			}
			
			PluginManager<ExecutionEnginePlugin> pluginManager;
			try {
				pluginManager = pluginBuilder.build();
			} catch (CircularDependencyException e) {
				throw new ExecutionEngineException(e);
			}
			
			ExecutionEngineContext executionEngineContext = new ExecutionEngineContext(operationMode);
			if(parentContext != null) {
				executionEngineContext.useAllAttributesFromParentContext(parentContext);
			}
			ExecutionEnginePlugin plugins = pluginManager.getProxy();
			plugins.initializeExecutionEngineContext(parentContext, executionEngineContext);
			
			return new ExecutionEngine(executionEngineContext, plugins);
		}
	}
	
	/**
	 * @return a new instance of the {@link ExecutionEngine} build
	 */
	public static Builder builder() {
		return new Builder();
	}
	
	/**
	 * @return the {@link ExecutionEngineContext} of this {@link ExecutionEngine}
	 */
	public ExecutionEngineContext getExecutionEngineContext() {
		return executionEngineContext;
	}
	
	/**
	 * Initializes an execution based on the provided parameters.
	 * This creates and persists a new {@link Execution} instance.
	 * This method call should be followed by {@link ExecutionEngine#execute(String)}
	 * 
	 * @param executionParameters the {@link ExecutionParameters} of the {@link Execution} to be created
	 * @return the ID of the new {@link Execution}
	 */
	public String initializeExecution(ExecutionParameters executionParameters) {
		return initializeExecution(executionParameters, null);
	}
	
	/**
	 * Initializes an execution based on the provided parameters.
	 * This creates and persists a new {@link Execution} instance.
	 * This method call should be followed by {@link ExecutionEngine#execute(String)}
	 * 
	 * @param executionParameters the {@link ExecutionParameters} of the {@link Execution} to be created
	 * @param taskID the ID of the scheduler task from which this execution is originating from
	 * @return the ID of the new {@link Execution}
	 */
	public String initializeExecution(ExecutionParameters executionParameters, String taskID) {
		Execution execution = new ExecutionFactory().createExecution(executionParameters, taskID);
		executionEngineContext.getExecutionAccessor().save(execution);
		String executionId = execution.getId().toString();
		return executionId;
	}
	
	/**
	 * Executes an {@link Execution} previously initialized by {@link ExecutionEngine#initializeExecution(ExecutionParameters, String)} 
	 * @param executionId the ID of the {@link Execution} initialized and returned by {@link ExecutionEngine#initializeExecution(ExecutionParameters, String)}
	 * @return the result of the execution
	 */
	public PlanRunnerResult execute(String executionId) {
		logger.info("Starting execution with id '"+executionId+"'");
		Execution execution = executionEngineContext.getExecutionAccessor().get(executionId);
		if(execution != null) {
			ExecutionParameters executionParameters = execution.getExecutionParameters();
			return execute(executionId, executionParameters);
		} else {
			throw new ExecutionEngineException("Unable to find execution with ID '"+executionId+"'. Please ensure that you've called initializeExecution() first");
		}
	}
	
	/**
	 * Create a new execution and runs the provided plan as part of this {@link Execution}
	 * @param plan the {@link Plan} to be executed
	 * @return
	 */
	public PlanRunnerResult execute(Plan plan) {
		return execute(plan, null);
	}
	
	/**
	 * Create a new execution and runs the provided plan as part of this {@link Execution}
	 * @param plan the {@link Plan} to be executed
	 * @param executionParameters the map of execution parameters. These parameters are equivalent to 
	 * the parameters selected on the execution screen of the STEP UI
	 * @return
	 */
	public PlanRunnerResult execute(Plan plan, Map<String, String> executionParameters) {
		ExecutionParameters executionParameterObject = new ExecutionParameters(plan, executionParameters);
		return execute(executionParameterObject);
	}

	/**
	 * Create a new execution and runs the plan specified within the provided {@link ExecutionParameters}
	 * @param executionParameterObject the 
	 * @return
	 */
	public PlanRunnerResult execute(ExecutionParameters executionParameterObject) {
		String executionId = initializeExecution(executionParameterObject, null);
		return execute(executionId, executionParameterObject);
	}
	
	private PlanRunnerResult execute(String executionId, ExecutionParameters executionParameters) {
		ExecutionContext context = newExecutionContext(executionId, executionParameters);
		ExecutionEngineRunner executionEngineRunner = new ExecutionEngineRunner(context);
		currentExecutions.put(executionId, context);
		try {
			return executionEngineRunner.execute(); 
		} finally {
			currentExecutions.remove(executionId);
		}
	}
	
	/**
	 * @return a new {@link ExecutionContext}. <b>Warning:</b> This method should only be used for testing purposes 
	 * i.e. for tests requiring an {@link ExecutionContext} without {@link ExecutionEngine}
	 */
	public ExecutionContext newExecutionContext() {
		return newExecutionContext(new ObjectId().toString(), new ExecutionParameters());
	}
	
	protected ExecutionContext newExecutionContext(String executionId, ExecutionParameters executionParameters) {
		ExecutionContext executionContext = new ExecutionContext(executionId, executionParameters);
		executionContext.useStandardAttributesFromParentContext(executionEngineContext);
		executionContext.useReportingAttributesFromParentContext(executionEngineContext);
		executionContext.useSourceAttributesFromParentContext(executionEngineContext);

		// Use a layered plan accessor to isolate the local context from the parent one
		// This allow temporary persistence of plans for the duration of the execution
		LayeredPlanAccessor planAccessor = new LayeredPlanAccessor();
		planAccessor.pushAccessor(executionEngineContext.getPlanAccessor());
		planAccessor.pushAccessor(new InMemoryPlanAccessor());
		executionContext.setPlanAccessor(planAccessor);
		
		executionContext.setExecutionCallbacks(plugins);
		plugins.initializeExecutionContext(executionEngineContext, executionContext);
		
		return executionContext;
	}

	/**
	 * @return the {@link List} of currently running executions
	 */
	public List<ExecutionContext> getCurrentExecutions() {
		return new ArrayList<>(currentExecutions.values());
	}
}
