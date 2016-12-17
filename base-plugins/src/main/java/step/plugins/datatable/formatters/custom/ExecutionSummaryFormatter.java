package step.plugins.datatable.formatters.custom;

import org.bson.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.execution.model.Execution;
import step.plugins.datatable.formatters.Formatter;
import step.plugins.views.ViewModelAccessor;
import step.plugins.views.functions.ReportNodeStatusDistribution;

public class ExecutionSummaryFormatter implements Formatter {
	
	ObjectMapper mapper = new ObjectMapper();
	
	ViewModelAccessor viewModelAccessor;

	public ExecutionSummaryFormatter(GlobalContext context) {
		super();
		this.viewModelAccessor = new ViewModelAccessor(context.getMongoClient(), context.getMongoDatabase());;
	}

	@Override
	public String format(Object value, Document row) {
		String eid = row.get("_id").toString();
		ReportNodeStatusDistribution distribution = viewModelAccessor.get("statusDistributionForFunctionCalls", eid, ReportNodeStatusDistribution.class);
		if(distribution!=null) {
			try {
				return mapper.writeValueAsString(distribution);
			} catch (JsonProcessingException e1) {
				throw new RuntimeException("Error while writing distribution",e1);
			}			
		} else {
			return "{}";
		}
	}

	@Override
	public Object parse(String formattedValue) {
		// TODO Auto-generated method stub
		return null;
	}

}
