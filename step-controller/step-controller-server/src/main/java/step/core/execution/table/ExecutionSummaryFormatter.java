package step.core.execution.table;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.execution.type.ExecutionType;
import step.core.execution.type.ExecutionTypeManager;

public class ExecutionSummaryFormatter {
				
	ExecutionTypeManager executionTypeManager;
	
	private static final Logger logger = LoggerFactory.getLogger(ExecutionSummaryFormatter.class);

	public ExecutionSummaryFormatter(GlobalContext context) {
		super();
		executionTypeManager = context.get(ExecutionTypeManager.class);
	}

	public String format(Document row) {
		String eid = row.get("_id").toString();
		
		String executionTypeName = row.containsKey("executionType")?row.get("executionType").toString():null;
		ExecutionType executionType = executionTypeManager.get(executionTypeName);
		try {
			if(executionType!=null) {
				String result = executionType.getExecutionSummary(eid);
				if(result!=null) {
					return result;				
				} else {
					logger.warn("Execution summary not available for execution "+eid);
				}				
			} else {
				logger.warn("Execution type "+executionTypeName+ " not available");
			}
		} catch (Exception e) {
			logger.error("Error while getting execution summary for execution "+eid, e);
		}			
		return "{}";
	}
}
