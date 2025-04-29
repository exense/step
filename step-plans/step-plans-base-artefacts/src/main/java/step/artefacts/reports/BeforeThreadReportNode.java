package step.artefacts.reports;

import step.core.artefacts.reports.ReportNode;

/**
 * Required for the housekeeping job (executions created prior to the deprecation / removal of this class)
 * Used only for report node deserialization
 */
@Deprecated
public class BeforeThreadReportNode extends ReportNode {

	@Override
	public boolean setVariableInParentScope() {
		return true;
	}
}
