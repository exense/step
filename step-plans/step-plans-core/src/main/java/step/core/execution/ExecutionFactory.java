package step.core.execution;

import java.util.HashMap;

import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;

public class ExecutionFactory {
	
	public Execution createExecution(ExecutionParameters executionParameters, String taskID) {		
		Execution execution = new Execution();
		execution.setStartTime(System.currentTimeMillis());
		execution.setExecutionParameters(executionParameters);
		execution.setStatus(ExecutionStatus.INITIALIZING);
		execution.setAttributes(new HashMap<>());
		
		if(executionParameters.getAttributes() != null) {
			execution.getAttributes().putAll(executionParameters.getAttributes());
		}

//		if(objectEnricher != null) {
//			objectEnricher.accept(execution);
//		}
		
		execution.setExecutionTaskID(taskID);
		
		if(executionParameters.getDescription()!=null) {
			execution.setDescription(executionParameters.getDescription());
		}

		return execution;
	}
}
