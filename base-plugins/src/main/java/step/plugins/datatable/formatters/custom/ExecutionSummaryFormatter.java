package step.plugins.datatable.formatters.custom;

import org.bson.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.plugins.datatable.formatters.Formatter;
import step.plugins.views.ViewPlugin;
import step.plugins.views.functions.ReportNodeStatusDistribution;

public class ExecutionSummaryFormatter implements Formatter {
	
	ObjectMapper mapper = new ObjectMapper();
		
	ViewPlugin viewPlugin;

	public ExecutionSummaryFormatter(GlobalContext context) {
		super();
		this.viewPlugin = (ViewPlugin) context.get(ViewPlugin.VIEW_PLUGIN_KEY);
		
	}

	@Override
	public String format(Object value, Document row) {
		String eid = row.get("_id").toString();
		ReportNodeStatusDistribution distribution = (ReportNodeStatusDistribution) viewPlugin.query("statusDistributionForFunctionCalls", eid);
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
