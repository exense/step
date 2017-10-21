package step.plugins.executiontypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.execution.type.ExecutionType;
import step.plugins.views.ViewPlugin;
import step.plugins.views.functions.ReportNodeStatusDistribution;

public class TestSetExecutionType extends ExecutionType {

	ViewPlugin viewPlugin;
	
	ObjectMapper mapper = new ObjectMapper();
	
	public TestSetExecutionType(GlobalContext context) {
		super("TestSet");
		this.viewPlugin = (ViewPlugin) context.get(ViewPlugin.VIEW_PLUGIN_KEY);
	}

	@Override
	public String getExecutionSummary(String executionId) {
		ReportNodeStatusDistribution distribution = (ReportNodeStatusDistribution) viewPlugin.query("statusDistributionForTestcases", executionId);
		if(distribution!=null) {
			try {
				return mapper.writeValueAsString(distribution);
			} catch (JsonProcessingException e1) {
				throw new RuntimeException("Error while writing distribution",e1);
			}			
		} else {
			return null;
		}
	}

}
