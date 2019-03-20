package step.plugins.datatable.formatters.custom;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.execution.type.ExecutionType;
import step.core.execution.type.ExecutionTypeManager;
import step.plugins.datatable.formatters.Formatter;

public class ExecutionSummaryFormatter implements Formatter {
				
	ExecutionTypeManager executionTypeManager;
	
	private static final Logger logger = LoggerFactory.getLogger(ExecutionSummaryFormatter.class);

	public ExecutionSummaryFormatter(GlobalContext context) {
		super();
		executionTypeManager = context.get(ExecutionTypeManager.class);
	}

	@Override
	public String format(Object value, Document row) {
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

	@Override
	public Object parse(String formattedValue) {
		throw new RuntimeException("Not implemented");
	}

}
