package step.core.artefacts.reports.aggregated;

import java.util.List;

public class FlatAggregatedReport {
    public List<AggregatedReportView> aggregatedReportViews;

    public FlatAggregatedReport(List<AggregatedReportView> aggregatedReportViews) {
        this.aggregatedReportViews = aggregatedReportViews;
    }

    public FlatAggregatedReport() {
    }
}
