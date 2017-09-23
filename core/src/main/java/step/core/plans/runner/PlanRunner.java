package step.core.plans.runner;

import step.core.artefacts.reports.ReportNode;
import step.core.plans.Plan;

public interface PlanRunner {

	public ReportNode run(Plan plan);
}
