package step.core.execution;

import java.util.List;

import org.bson.types.ObjectId;

import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.runner.PlanRunner;
import step.core.plugins.PluginManager;
import step.core.plugins.PluginManager.Builder;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;
import step.engine.PlanRunnerImpl;
import step.engine.plugins.ExecutionEnginePlugin;

public class ExecutionEngine {

	private ExecutionEngineContext executionEngineContext;
	private final PluginManager<ExecutionEnginePlugin> pluginManager;
	private ExecutionEnginePlugin plugins;

	public ExecutionEngine() {
		this(OperationMode.LOCAL, null);
	}
	
	public ExecutionEngine(OperationMode operationMode, AbstractExecutionEngineContext parentContext) {
		this(operationMode, parentContext, null);
	}
	
	public ExecutionEngine(OperationMode operationMode, AbstractExecutionEngineContext parentContext, List<ExecutionEnginePlugin> addtionalPlugins) {
		super();
		
		Builder<ExecutionEnginePlugin> build = PluginManager.builder(ExecutionEnginePlugin.class);
		if(addtionalPlugins != null) {
			build.withPlugins(addtionalPlugins);
		}
		try {
			pluginManager = build.withPluginsFromClasspath().build();
		} catch (InstantiationException | IllegalAccessException | CircularDependencyException e) {
			throw new ExecutionEngineException(e);
		}
	
		executionEngineContext = new ExecutionEngineContext(operationMode, parentContext);
		
		plugins = pluginManager.getProxy();
		plugins.initialize(executionEngineContext);
	}
	
	public ExecutionEngineContext getExecutionEngineContext() {
		return executionEngineContext;
	}

	public ExecutionContext newExecutionContext(String executionId, ExecutionParameters executionParameters) {
		ExecutionContext executionContext = new ExecutionContext(executionEngineContext, executionId, executionParameters);
		executionContext.setExecutionCallbacks(plugins);
		return executionContext;
	}

	public ExecutionContext newExecutionContext(ExecutionParameters executionParameters) {
		return newExecutionContext(new ObjectId().toString(), executionParameters);
	}
	
	public ExecutionContext newExecutionContext() {
		return newExecutionContext(new ExecutionParameters("dummy", null, ExecutionMode.RUN));
	}
	
	public PlanRunner getPlanRunner() {
		return getPlanRunner(newExecutionContext());
	}
	
	public PlanRunner getPlanRunner(ExecutionContext executionContext) {
		return new PlanRunnerImpl(executionContext);
	}
}
