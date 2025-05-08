package step.core.artefacts.reports.aggregated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class AggregatedReportViewRequest {
    public final ReportNodeTimeSeries.Range range;
    public final Boolean resolveSingleInstanceReport; //keep null, will be replaced by system default in such cases
    public final String selectedReportNodeId; // request a partial aggregated report for the selected report node, Note if the node is part of an iteration the partial tree contain the whole iteration
    public final boolean filterResolvedPlanNodes; //only used for partial tree, default true, only return the subtree for provided selectedReportNodeId
    public final List<String> filterArtefactClasses; //filter the report for provided artefact class
    public final boolean fetchCurrentOperations; //fetch current operations for all RUNNING nodes

    public AggregatedReportViewRequest() {
        this(null, null, null, null, null, false);
    }

    public AggregatedReportViewRequest(ReportNodeTimeSeries.Range range,
                                       Boolean resolveSingleInstanceReport,
                                       String selectedReportNodeId,
                                       Boolean filterResolvedPlanNodes,
                                       List<String> filterArtefactClasses) {
        this(range, resolveSingleInstanceReport, selectedReportNodeId, filterResolvedPlanNodes, filterArtefactClasses, false);
    }

    @JsonCreator
    public AggregatedReportViewRequest(@JsonProperty("range") ReportNodeTimeSeries.Range range,
                                       @JsonProperty("resolveSingleInstanceReport") Boolean resolveSingleInstanceReport,
                                       @JsonProperty("selectedReportNodeId") String selectedReportNodeId,
                                       @JsonProperty("filterResolvedPlanNodes") Boolean filterResolvedPlanNodes,
                                       @JsonProperty("filterArtefactClasses") List<String> filterArtefactClasses,
                                       @JsonProperty("fetchCurrentOperations") boolean fetchCurrentOperations) {
        this.range = range;
        this.resolveSingleInstanceReport = resolveSingleInstanceReport;
        this.selectedReportNodeId = selectedReportNodeId;
        this.filterResolvedPlanNodes = (filterResolvedPlanNodes != null) ? filterResolvedPlanNodes : true;
        this.filterArtefactClasses = (filterArtefactClasses != null) ? filterArtefactClasses : new ArrayList<>();
        this.fetchCurrentOperations = fetchCurrentOperations;
    }
}
