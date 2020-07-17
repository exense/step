package step.plugins.executiontypes;

import step.core.GlobalContext;
import step.core.execution.type.ExecutionType;
import step.plugins.views.ViewManager;
import step.plugins.views.functions.ReportNodeStatusDistribution;

public class TestSetExecutionType extends ExecutionType {

	public static final String NAME = "TestSet";

	private ViewManager viewManager;
	
	public TestSetExecutionType(GlobalContext context) {
		super(NAME);
		this.viewManager = context.get(ViewManager.class);
	}

	@Override
	public Object getExecutionSummary(String executionId) {
		ReportNodeStatusDistribution distribution = (ReportNodeStatusDistribution) viewManager.queryView("statusDistributionForTestcases", executionId);
		return distribution;
	}

}
