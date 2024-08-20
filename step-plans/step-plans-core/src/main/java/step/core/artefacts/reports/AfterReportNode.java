package step.core.artefacts.reports;

public class AfterReportNode extends ReportNode {

    @Override
    public boolean setVariableInParentScope() {
        return true;
    }
}
