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

import step.core.AbstractContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionParameters;
import step.core.objectenricher.*;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.LayeredPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanTypeRegistry;
import step.core.plans.runner.PlanRunner;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.Plugin;
import step.core.plugins.PluginManager;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;
import step.core.scheduler.ExecutiontTaskParameters;
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
	
	private final ObjectHookRegistry objectHookRegistry;
	private final ExecutionEngineContext executionEngineContext;
	private final ExecutionEnginePlugin plugins;
	private final ConcurrentHashMap<String, ExecutionContext> currentExecutions = new ConcurrentHashMap<>();
	
	private ExecutionEngine(ExecutionEngineContext executionEngineContext, ExecutionEnginePlugin plugins, ObjectHookRegistry objectHookRegistry) {
		super();
		
		this.executionEngineContext = executionEngineContext;
		this.plugins = plugins;
		if(objectHookRegistry != null) {
			this.objectHookRegistry = objectHookRegistry;
		} else {
			this.objectHookRegistry = new ObjectHookRegistry();
		}
	}
	
	public static class Builder {

		private final step.core.plugins.PluginManager.Builder<ExecutionEnginePlugin> pluginBuilder;
		private OperationMode operationMode;
		private AbstractExecutionEngineContext parentContext;
		private ObjectHookRegistry objectHookRegistry;
		private PlanTypeRegistry planTypeRegistry;

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
		 * Use a specific {@link ObjectHookRegistry} to handle object enrichment and filtering
		 * @param objectHookRegistry the {@link ObjectHookRegistry} to be used
		 * @return 
		 */
		public Builder withObjectHookRegistry(ObjectHookRegistry objectHookRegistry) {
			this.objectHookRegistry = objectHookRegistry;
			return this;
		}

		/**
		 * Use a specific {@link PlanTypeRegistry}
		 */
		public Builder withPlanTypeRegistry(PlanTypeRegistry planTypeRegistry) {
			this.planTypeRegistry = planTypeRegistry;
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
			if(objectHookRegistry != null) {
				executionEngineContext.put(ObjectHookRegistry.class, objectHookRegistry);
			}
			if(planTypeRegistry != null) {
				executionEngineContext.put(PlanTypeRegistry.class, planTypeRegistry);
			}
			ExecutionEnginePlugin plugins = pluginManager.getProxy();
			plugins.initializeExecutionEngineContext(parentContext, executionEngineContext);
			
			return new ExecutionEngine(executionEngineContext, plugins, objectHookRegistry);
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
		Execution execution = ExecutionFactory.createExecution(executionParameters, null, getObjectEnricher(executionParameters, null));
		return saveExecution(execution);
	}
	
	/**
	 * Initializes an execution based on the provided parameters.
	 * This creates and persists a new {@link Execution} instance.
	 * This method call should be followed by {@link ExecutionEngine#execute(String)}
	 * 
	 * @param executionTaskParameters the {@link ExecutiontTaskParameters} of the {@link Execution} to be created
	 * @return the ID of the new {@link Execution}
	 */
	public String initializeExecution(ExecutiontTaskParameters executionTaskParameters) {
		Execution execution = ExecutionFactory.createExecution(executionTaskParameters.getExecutionsParameters(), executionTaskParameters, getObjectEnricher(null, executionTaskParameters));
		return saveExecution(execution);
	}

	private String saveExecution(Execution execution) {
		executionEngineContext.getExecutionAccessor().save(execution);
		String executionId = execution.getId().toString();
		return executionId;
	}
	
	/**
	 * Executes an {@link Execution} previously initialized by {@link ExecutionEngine#initializeExecution(ExecutionParameters)}
	 * @param executionId the ID of the {@link Execution} initialized and returned by {@link ExecutionEngine#initializeExecution(ExecutionParameters)}
	 * @return the result of the execution
	 */
	public PlanRunnerResult execute(String executionId) {
		logger.info("Starting execution with id '"+executionId+"'");
		Execution execution = executionEngineContext.getExecutionAccessor().get(executionId);
		if(execution != null) {
			ExecutionParameters executionParameters = execution.getExecutionParameters();
			ExecutiontTaskParameters executiontTaskParameters = execution.getExecutiontTaskParameters();
			return execute(executionId, executionParameters, executiontTaskParameters);
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
		String executionId = initializeExecution(executionParameterObject);
		return execute(executionId, executionParameterObject, null);
	}
	
	private PlanRunnerResult execute(String executionId, ExecutionParameters executionParameters, ExecutiontTaskParameters executiontTaskParameters) {
		ExecutionContext context = newExecutionContext(executionId, executionParameters, executiontTaskParameters);
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
		return newExecutionContext(new ObjectId().toString(), new ExecutionParameters(), null);
	}
	
	protected ExecutionContext newExecutionContext(String executionId, ExecutionParameters executionParameters, ExecutiontTaskParameters executiontTaskParameters) {
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
		
		addObjectHooksToExecutionContext(executionParameters, executiontTaskParameters, executionContext);
		
		return executionContext;
	}
	
	private ObjectEnricher getObjectEnricher(ExecutionParameters executionParameters, ExecutiontTaskParameters executiontTaskParameters) {
		ObjectHookContext context = rebuildObjectHookContext(executionParameters, executiontTaskParameters);
		return objectHookRegistry.getObjectEnricher(context);
	}
	
	private ObjectPredicate getObjectPredicate(ExecutionParameters executionParameters, ExecutiontTaskParameters executiontTaskParameters) {
		ObjectHookContext context = rebuildObjectHookContext(executionParameters, executiontTaskParameters);
		ObjectPredicateFactory objectPredicateFactory = new ObjectPredicateFactory(objectHookRegistry);
		return objectPredicateFactory.getObjectPredicate(context);
	}
	
	private void addObjectHooksToExecutionContext(ExecutionParameters executionParameters, ExecutiontTaskParameters executiontTaskParameters, ExecutionContext executionContext) {
		ObjectEnricher objectEnricher = getObjectEnricher(executionParameters, executiontTaskParameters);
		ObjectPredicate objectPredicate = getObjectPredicate(executionParameters, executiontTaskParameters);
		executionContext.setObjectPredicate(objectPredicate);
		executionContext.setObjectEnricher(objectEnricher);
	}

	private ObjectHookContext rebuildObjectHookContext(ExecutionParameters executionParameters, ExecutiontTaskParameters executiontTaskParameters) {
		// Rebuild the Session based on the relevant object (ExecutionParameters or ExecutiontTaskParameters)
		// This has to be done because the Session is not always available when running an execution
		// (by Scheduled tasks for instance)
		EnricheableObject sessionRelevantObject;
		if(executiontTaskParameters != null) {
			sessionRelevantObject = executiontTaskParameters;
		} else {
			sessionRelevantObject = executionParameters;
		}
		
		ObjectHookContext context = new ObjectHookContext();
		try {
			objectHookRegistry.rebuildContext(context, sessionRelevantObject);
		} catch (Exception e) {
			String errorMessage = "Error while rebuilding context for origin object "+sessionRelevantObject;
			logger.error(errorMessage, e);
			throw new RuntimeException(errorMessage, e);
		}
		return context;
	}
	
	private static class ObjectHookContext extends AbstractContext {}

	/**
	 * @return the {@link List} of currently running executions
	 */
	public List<ExecutionContext> getCurrentExecutions() {
		return new ArrayList<>(currentExecutions.values());
	}
}
