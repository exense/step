package step.core.execution;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.PluginManager;
import step.core.plugins.PluginManager.Builder;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;
import step.engine.plugins.ExecutionEnginePlugin;

public class ExecutionEngine {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionEngine.class);
	
	private final ExecutionEngineContext executionEngineContext;
	private final PluginManager<ExecutionEnginePlugin> pluginManager;
	private final ExecutionEnginePlugin plugins;

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
		} catch (InstantiationException | IllegalAccessException | CircularDependencyException | ClassNotFoundException e) {
			throw new ExecutionEngineException(e);
		}
		plugins = pluginManager.getProxy();
	
		executionEngineContext = new ExecutionEngineContext(operationMode);
		executionEngineContext.useAllAttributesFromParentContext(parentContext);
		plugins.initializeExecutionEngineContext(parentContext, executionEngineContext);
	}
	
	public ExecutionEngineContext getExecutionEngineContext() {
		return executionEngineContext;
	}

	public ExecutionContext newExecutionContext(String executionId, ExecutionParameters executionParameters) {
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

	public ExecutionContext newExecutionContext(ExecutionParameters executionParameters) {
		return newExecutionContext(new ObjectId().toString(), executionParameters);
	}
	
	public ExecutionContext newExecutionContext() {
		return newExecutionContext(new ExecutionParameters());
	}
	
	public PlanRunnerResult execute(Plan plan) {
		return execute(plan, null);
	}
	
	public PlanRunnerResult execute(Plan plan, Map<String, String> executionParameters) {
		ExecutionParameters executionParameterObject = new ExecutionParameters(plan, executionParameters);
		ExecutionContext executionContext = newExecutionContext(executionParameterObject);
		return new ExecutionEngineRunner(executionContext).execute();
	}
	
	public PlanRunnerResult execute(ExecutionContext context) {
		ExecutionEngineRunner executionEngineRunner = new ExecutionEngineRunner(context);
		return executionEngineRunner.execute(); 
	}
}
