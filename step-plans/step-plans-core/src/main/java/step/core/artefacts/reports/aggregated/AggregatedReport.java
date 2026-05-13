package step.core.artefacts.reports.aggregated;

public class AggregatedReport {
    public AggregatedReportView aggregatedReportView;
    public String partialTreeRootNodeId;

    public AggregatedReport(AggregatedReportView aggregatedReportView) {
        this.aggregatedReportView = aggregatedReportView;
    }

    public AggregatedReport() {

    }
}
