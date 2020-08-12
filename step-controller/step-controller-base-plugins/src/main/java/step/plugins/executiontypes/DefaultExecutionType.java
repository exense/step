package step.plugins.executiontypes;

import step.core.GlobalContext;
import step.core.execution.type.ExecutionType;
import step.plugins.views.ViewManager;
import step.plugins.views.functions.ReportNodeStatusDistribution;

public class DefaultExecutionType extends ExecutionType {

	private ViewManager viewManager;
	
	public DefaultExecutionType(GlobalContext context) {
		super("Default");
		this.viewManager = context.get(ViewManager.class);
	}

	@Override
	public Object getExecutionSummary(String executionId) {
		ReportNodeStatusDistribution distribution = (ReportNodeStatusDistribution) viewManager.queryView("statusDistributionForFunctionCalls", executionId);
		return distribution;
	}

}
