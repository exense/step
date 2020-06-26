package step.core.execution;

import step.core.GlobalContext;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.threadpool.ThreadPool;

public class ControllerExecutionContextBuilder extends ExecutionContextBuilder {

	public ControllerExecutionContextBuilder() {
		super();
	}

	public ControllerExecutionContextBuilder(String executionId) {
		super(executionId);
	}

	public ControllerExecutionContextBuilder(String executionId, ExecutionParameters executionParameters) {
		super(executionId, executionParameters);
	}
	
	public ControllerExecutionContextBuilder configureForControllerExecution(GlobalContext globalContext) {
		ExecutionParameters executionParameters = executionContext.getExecutionParameters();
		boolean isolatedContext = executionParameters.isIsolatedExecution();
		if(isolatedContext) {
			configureForlocalExecution();
			executionContext.setReportNodeAccessor(globalContext.getReportAccessor());
			executionContext.setEventManager(globalContext.getEventManager());
			executionContext.setExecutionCallbacks(globalContext.getPluginManager().getProxy());
			ExecutionManager executionManager = new ExecutionManagerImpl(globalContext.getExecutionAccessor());
			executionContext.put(ExecutionManager.class, executionManager);
		} else {
			executionContext.setExpressionHandler(globalContext.getExpressionHandler());
			executionContext.setDynamicBeanResolver(globalContext.getDynamicBeanResolver());
			executionContext.setConfiguration(globalContext.getConfiguration());
			executionContext.setPlanAccessor(globalContext.getPlanAccessor());
			executionContext.setReportNodeAccessor(globalContext.getReportAccessor());
			executionContext.setEventManager(globalContext.getEventManager());
			executionContext.setExecutionCallbacks(globalContext.getPluginManager().getProxy());
			ExecutionManager executionManager = new ExecutionManagerImpl(globalContext.getExecutionAccessor());
			executionContext.put(ExecutionManager.class, executionManager);
			executionContext.put(ThreadPool.class, new ThreadPool(executionContext));
			executionContext.setExecutionTypeListener(executionManager);
			
		}
		executionContext.updateStatus(ExecutionStatus.INITIALIZING);
		return this;
	}
}
