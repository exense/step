package step.artefacts.reports;

import step.core.artefacts.reports.ReportNode;

public class BeforeThreadReportNode extends ReportNode {

	@Override
	public boolean setVariableInParentScope() {
		return true;
	}
}
