package step.core.execution;

import step.core.GlobalContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;

public class ExecutionRunnableFactory {
		
	private GlobalContext globalContext;
	
	public ExecutionRunnableFactory(GlobalContext globalContext) {
		super();
		this.globalContext = globalContext;
	}
	
	public ExecutionRunnable newExecutionRunnable(Execution execution) {		
		ExecutionContext context = createExecutionContext(execution);
		ExecutionRunnable task = new ExecutionRunnable(context);
		return task;
	}

	private ExecutionContext createExecutionContext(Execution execution) {
		ExecutionContext context = new ExecutionContext(execution.getId());
		context.setGlobalContext(globalContext);
		context.updateStatus(ExecutionStatus.INITIALIZING);
		context.setExecutionParameters(execution.getExecutionParameters());
		
		return context;
	}
	
	public Execution createExecution(ExecutionParameters executionParameters, String taskID) {		
		Execution execution = new Execution();
		execution.setStartTime(System.currentTimeMillis());
		execution.setExecutionParameters(executionParameters);
		execution.setStatus(ExecutionStatus.INITIALIZING);

		if(taskID!=null) {
			execution.setExecutionTaskID(taskID);
		}
		
		if(executionParameters.getDescription()!=null) {
			execution.setDescription(executionParameters.getDescription());
		}
		
		globalContext.getExecutionAccessor().save(execution);		
		return execution;
	}

}
