package step.core.execution.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.execution.model.Execution;
import step.core.execution.type.ExecutionType;
import step.core.execution.type.ExecutionTypeManager;

public class ExecutionSummaryProvider {
				
	private ExecutionTypeManager executionTypeManager;
	
	private static final Logger logger = LoggerFactory.getLogger(ExecutionSummaryProvider.class);

	public ExecutionSummaryProvider(GlobalContext context) {
		super();
		executionTypeManager = context.get(ExecutionTypeManager.class);
	}

	public Object format(Execution execution) {
		String executionTypeName = execution.getExecutionType();
		ExecutionType executionType = executionTypeManager.get(executionTypeName);
		try {
			if(executionType!=null) {
				Object result = executionType.getExecutionSummary(execution.getId().toString());
				return result;				
			} else {
				logger.warn("Execution type "+executionTypeName+ " not available");
			}
		} catch (Exception e) {
			logger.error("Error while getting execution summary for execution "+execution.getId().toString(), e);
		}			
		return null;
	}
}
