package step.core.execution;

import org.bson.types.ObjectId;

import step.core.GlobalContext;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.threadpool.ThreadPool;

public class ControllerExecutionContextBuilder {

	private final GlobalContext globalContext;
	
	public ControllerExecutionContextBuilder(GlobalContext globalContext) {
		super();
		this.globalContext = globalContext;
	}

	public ExecutionContext createExecutionContext() {
		return createExecutionContext(new ObjectId().toString());
	}
	
	public ExecutionContext createExecutionContext(String executionId) {
		return createExecutionContext(executionId, new ExecutionParameters("dummy", null, ExecutionMode.RUN));
	}
	
	public ExecutionContext createExecutionContext(String executionId, ExecutionParameters executionParameters) {
		boolean isolatedContext = executionParameters.isIsolatedExecution();
		
		ExecutionContext context;
		if(isolatedContext) {
			context = ContextBuilder.createLocalExecutionContext(executionId);
			context.setReportNodeAccessor(globalContext.getReportAccessor());
			context.setEventManager(globalContext.getEventManager());
			context.setExecutionCallbacks(globalContext.getPluginManager().getProxy());

			ExecutionManager executionManager = new ExecutionManagerImpl(globalContext.getExecutionAccessor());
			context.put(ExecutionManager.class, executionManager);
		} else {
			context = new ExecutionContext(executionId);
			context.setExpressionHandler(globalContext.getExpressionHandler());
			context.setDynamicBeanResolver(globalContext.getDynamicBeanResolver());
			context.setConfiguration(globalContext.getConfiguration());
			context.setPlanAccessor(globalContext.getPlanAccessor());
			context.setReportNodeAccessor(globalContext.getReportAccessor());
			context.setEventManager(globalContext.getEventManager());
			context.setExecutionCallbacks(globalContext.getPluginManager().getProxy());
			ExecutionManager executionManager = new ExecutionManagerImpl(globalContext.getExecutionAccessor());
			context.put(ExecutionManager.class, executionManager);
			context.put(ThreadPool.class, new ThreadPool(context));
			context.setExecutionTypeListener(executionManager);
			
		}
		context.setExecutionParameters(executionParameters);
		context.updateStatus(ExecutionStatus.INITIALIZING);
		return context;
	}
}
