package step.plugins.executiontypes;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.execution.type.ExecutionType;
import step.plugins.views.ViewPlugin;
import step.plugins.views.functions.ReportNodeStatusDistribution;

public class DefaultExecutionType extends ExecutionType {

	ViewPlugin viewPlugin;
	
	ObjectMapper mapper = new ObjectMapper();
	
	public DefaultExecutionType(GlobalContext context) {
		super("Default");
		this.viewPlugin = (ViewPlugin) context.get(ViewPlugin.VIEW_PLUGIN_KEY);
	}

	@Override
	public Object getExecutionSummary(String executionId) {
		ReportNodeStatusDistribution distribution = (ReportNodeStatusDistribution) viewPlugin.query("statusDistributionForFunctionCalls", executionId);
		return distribution;
	}

}
