package step.core.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.execution.model.ExecutionStatus;

public class ExecutionLifecycleManager {

	private final GlobalContext context;
	
	private static final Logger logger = LoggerFactory.getLogger(ExecutionLifecycleManager.class);
	
	public ExecutionLifecycleManager(GlobalContext context) {
		super();
		this.context = context;
	}

	public void abort(ExecutionRunnable task) {
		if(task!=null && task.getContext().getStatus()!=ExecutionStatus.ENDED) {
			ExecutionStatusManager.updateStatus(task.getContext(), ExecutionStatus.ABORTING);
		}
		context.getPluginManager().getProxy().beforeExecutionEnd(task.getContext());
	}
	
	public void executionStarted(ExecutionRunnable task) {
		context.getPluginManager().getProxy().executionStart(task.getContext());
		ExecutionStatusManager.updateParameters(task.getContext());
	}
	
	public void executionEnded(ExecutionRunnable task) {
		context.getPluginManager().getProxy().afterExecutionEnd(task.getContext());
	}
	
	public void updateStatus(ExecutionRunnable runnable, ExecutionStatus newStatus) {
		ExecutionStatusManager.updateStatus(runnable.getContext(),newStatus);
	}
	
}
