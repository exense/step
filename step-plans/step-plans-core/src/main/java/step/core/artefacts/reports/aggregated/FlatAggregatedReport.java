package step.core.artefacts.reports.aggregated;

import java.util.List;

public class FlatAggregatedReport {
    public List<FlatAggregatedReportView> aggregatedReportViews;

    public FlatAggregatedReport(List<FlatAggregatedReportView> aggregatedReportViews) {
        this.aggregatedReportViews = aggregatedReportViews;
    }

    public FlatAggregatedReport() {
    }
}
