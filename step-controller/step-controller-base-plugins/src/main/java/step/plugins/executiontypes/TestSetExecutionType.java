package step.plugins.executiontypes;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.execution.type.ExecutionType;
import step.plugins.views.ViewPlugin;
import step.plugins.views.functions.ReportNodeStatusDistribution;

public class TestSetExecutionType extends ExecutionType {

	public static final String NAME = "TestSet";

	ViewPlugin viewPlugin;
	
	ObjectMapper mapper = new ObjectMapper();
	
	public TestSetExecutionType(GlobalContext context) {
		super(NAME);
		this.viewPlugin = (ViewPlugin) context.get(ViewPlugin.VIEW_PLUGIN_KEY);
	}

	@Override
	public Object getExecutionSummary(String executionId) {
		ReportNodeStatusDistribution distribution = (ReportNodeStatusDistribution) viewPlugin.query("statusDistributionForTestcases", executionId);
		return distribution;
	}

}
