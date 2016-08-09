package step.core.execution;

import java.util.HashMap;
import java.util.Map.Entry;

import step.core.artefacts.AbstractArtefact;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionStatus;

public class ExecutionStatusManager {
	
	protected static void updateParameters(ExecutionContext context) {
		ExecutionAccessor accessor = context.getGlobalContext().getExecutionAccessor();
		Execution execution = accessor.get(context.getExecutionId());
		
		HashMap<String, String> params = new HashMap<>();
		for(Entry<String, Object> entry:context.getVariablesManager().getAllVariables().entrySet()) {
			params.put(entry.getKey(), entry.getValue().toString());
		}
		execution.setParameters(params);
		
		accessor.save(execution);
	}
	
	protected static void persistStatus(ExecutionContext context) {
		ExecutionAccessor accessor = context.getGlobalContext().getExecutionAccessor();
		Execution execution = accessor.get(context.getExecutionId());
		if(context.getStatus()==ExecutionStatus.ENDED) {
			execution.setEndTime(System.currentTimeMillis());
		}
		execution.setStatus(context.getStatus());
		execution.setReportExports(context.getReportExports());
		if(context.getArtefact()!=null) {
			execution.setArtefactID(context.getArtefact().getId().toString());
			if(execution.getDescription()==null) {
				AbstractArtefact artefact = context.getArtefact();
				execution.setDescription(artefact.getAttributes()!=null?artefact.getAttributes().get("name"):null);
			}
		}
		accessor.save(execution);
	}
	
	protected static void updateStatus(ExecutionContext context, ExecutionStatus status) {
		context.updateStatus(status);
		persistStatus(context);
	}
	
}
