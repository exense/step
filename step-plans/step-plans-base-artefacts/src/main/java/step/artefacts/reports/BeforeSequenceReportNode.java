package step.artefacts.reports;

import step.core.artefacts.reports.ReportNode;

public class BeforeSequenceReportNode  extends ReportNode {

	@Override
	public boolean setVariableInParentScope() {
		return true;
	}
}
