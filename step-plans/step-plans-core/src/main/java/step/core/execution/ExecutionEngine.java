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
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.PluginManager;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;
import step.engine.plugins.ExecutionEnginePlugin;

public class ExecutionEngine {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionEngine.class);
	
	private final ExecutionEngineContext executionEngineContext;
	private final ExecutionEnginePlugin plugins;

	private ConcurrentHashMap<String, ExecutionContext> currentExecutions = new ConcurrentHashMap<>();
	
	private ExecutionEngine(ExecutionEngineContext executionEngineContext, ExecutionEnginePlugin plugins) {
		super();
		
		this.executionEngineContext = executionEngineContext;
		this.plugins = plugins;
	}
	
	public static class Builder {

		private step.core.plugins.PluginManager.Builder<ExecutionEnginePlugin> pluginBuilder = PluginManager.builder(ExecutionEnginePlugin.class);
		private OperationMode operationMode;
		private AbstractExecutionEngineContext parentContext;
		
		public Builder() {
			super();
		}
		
		public Builder withPlugin(ExecutionEnginePlugin plugin) {
			pluginBuilder.withPlugin(plugin);
			return this;
		}
	
		public Builder withPluginsFromClasspath() {
			try {
				pluginBuilder.withPluginsFromClasspath();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				throw new ExecutionEngineException(e);
			}
			return this;
		}
		
		public Builder withOperationMode(OperationMode operationMode) {
			this.operationMode = operationMode;
			return this;
		}
		
		public Builder withParentContext(AbstractExecutionEngineContext parentContext) {
			this.parentContext = parentContext;
			return this;
		}
		
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
	
	public static Builder builder() {
		return new Builder();
	}
	
	public ExecutionEngineContext getExecutionEngineContext() {
		return executionEngineContext;
	}
	
	public String initializeExecution(ExecutionParameters executionParameters, String taskID) {
		Execution execution = new ExecutionFactory().createExecution(executionParameters, taskID);
		executionEngineContext.getExecutionAccessor().save(execution);
		return execution.getId().toString();
	}
	
	public PlanRunnerResult execute(String executionId) {
		Execution execution = executionEngineContext.getExecutionAccessor().get(executionId);
		if(execution != null) {
			ExecutionParameters executionParameters = execution.getExecutionParameters();
			return execute(executionId, executionParameters);
		} else {
			throw new ExecutionEngineException("Unable to find execution with ID '"+executionId+"'. Please ensure that you've called initializeExecution() first");
		}
	}
	
	public PlanRunnerResult execute(Plan plan) {
		return execute(plan, null);
	}
	
	public PlanRunnerResult execute(Plan plan, Map<String, String> executionParameters) {
		ExecutionParameters executionParameterObject = new ExecutionParameters(plan, executionParameters);
		return execute(executionParameterObject);
	}

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
	
	public ExecutionContext newExecutionContext() {
		return newExecutionContext(new ObjectId().toString(), new ExecutionParameters());
	}
	
	protected ExecutionContext newExecutionContext(String executionId, ExecutionParameters executionParameters) {
		ExecutionContext executionContext = new ExecutionContext(executionId, executionParameters);
		executionContext.useStandardAttributesFromParentContext(executionEngineContext);
		executionContext.useReportingAttributesFromParentContext(executionEngineContext);
		if(!executionParameters.isIsolatedExecution()) {
			executionContext.useSourceAttributesFromParentContext(executionEngineContext);
		}
		executionContext.setExecutionCallbacks(plugins);
		plugins.initializeExecutionContext(executionEngineContext, executionContext);
		
		return executionContext;
	}

	public List<ExecutionContext> getCurrentExecutions() {
		return new ArrayList<>(currentExecutions.values());
	}
}
