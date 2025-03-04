package step.core.artefacts.reports.aggregated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.InMemoryReportNodeAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanNode;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanNodeAccessor;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.timeseries.TimeSeriesCollectionsSettings;

import java.util.*;
import java.util.stream.Collectors;

public class AggregatedReportViewBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AggregatedReportViewBuilder.class);
    public static final String EXECUTION_REPORT_AGGREGATED_TREE_RESOLVE_SINGLE_INSTANCE = "execution.report.aggregated-tree.resolve-single-instance";

    private final String executionId;
    private final ExecutionAccessor executionAccessor;
    private final ResolvedPlanNodeAccessor resolvedPlanNodeAccessor;
    private final ReportNodeTimeSeries mainReportNodesTimeSeries;
    private final ReportNodeAccessor mainReportNodeAccessor;
    private final boolean defaultResolveSingleInstanceReport;

    public AggregatedReportViewBuilder(ExecutionEngineContext executionEngineContext, String executionId) {
        this.executionId = executionId;
        this.executionAccessor = executionEngineContext.getExecutionAccessor();
        this.resolvedPlanNodeAccessor = executionEngineContext.require(ResolvedPlanNodeAccessor.class);
        this.mainReportNodeAccessor = executionEngineContext.getReportNodeAccessor();
        this.defaultResolveSingleInstanceReport =  executionEngineContext.getConfiguration().getPropertyAsBoolean(EXECUTION_REPORT_AGGREGATED_TREE_RESOLVE_SINGLE_INSTANCE, true);
        this.mainReportNodesTimeSeries = executionEngineContext.require(ReportNodeTimeSeries.class);
    }

    public static class AggregatedReport {
        public AggregatedReportView aggregatedReportView;
        public String resolvedPartialPath;

        public AggregatedReport(AggregatedReportView aggregatedReportView) {
            this.aggregatedReportView = aggregatedReportView;
        }

        public AggregatedReport() {

        }
    }

    public AggregatedReportView buildAggregatedReportView() {
        return buildAggregatedReportView(new AggregatedReportViewRequest(null, null, null, null));
    }

    public AggregatedReportView buildAggregatedReportView(AggregatedReportViewRequest request) {
        return buildAggregatedReport(request).aggregatedReportView;
    }

    public AggregatedReport buildAggregatedReport(AggregatedReportViewRequest request) {
        Objects.requireNonNull(request);
        Execution execution = executionAccessor.get(executionId);
        //Make sure the resolved Plan is available
        ResolvedPlanNode rootResolvedPlanNode = Optional.ofNullable(execution.getResolvedPlanRootNodeId()).map(resolvedPlanNodeAccessor::get).orElse(null);
        if (rootResolvedPlanNode == null) {
            return null;
        } else if (request.selectedReportNodeId == null) {
            // Generate complete aggregated report tree
            return new AggregatedReport(recursivelyBuildAggregatedReportTree(rootResolvedPlanNode, request, mainReportNodesTimeSeries, mainReportNodeAccessor, null));
        } else {
            // a node is selected to generate a partial aggregated report
            try (ReportNodeTimeSeries localReportNodesTimeSeries = getInMemoryReportNodeTimeSeries()) {
                InMemoryReportNodeAccessor inMemoryReportNodeAccessor = new InMemoryReportNodeAccessor();
                AggregatedReport aggregatedReport = new AggregatedReport();
                Set<String> reportArtefactHashSet = buildPartialReportNodeTimeSeries(aggregatedReport, request.selectedReportNodeId, localReportNodesTimeSeries, inMemoryReportNodeAccessor);
                // Only pass the reportArtefactHashSet if aggregate view filtering is enabled
                reportArtefactHashSet = (request.filterResolvedPlanNodes) ? reportArtefactHashSet : null;
                aggregatedReport.aggregatedReportView = recursivelyBuildAggregatedReportTree(rootResolvedPlanNode, request, localReportNodesTimeSeries, inMemoryReportNodeAccessor, reportArtefactHashSet);
                return aggregatedReport;
            }
        }
    }

    public static class AggregatedReportViewRequest {
        public final ReportNodeTimeSeries.Range range;
        public final Boolean resolveSingleInstanceReport; //keep null, will be replaced by system default in such cases
        public final String selectedReportNodeId;
        public final boolean filterResolvedPlanNodes;

        @JsonCreator
        public AggregatedReportViewRequest(@JsonProperty("range") ReportNodeTimeSeries.Range range,
                                           @JsonProperty("resolveSingleInstanceReport") Boolean resolveSingleInstanceReport,
                                           @JsonProperty("selectedReportNodeId") String selectedReportNodeId,
                                           @JsonProperty("filterResolvedPlanNodes") Boolean filterResolvedPlanNodes) {
            this.range = range;
            this.resolveSingleInstanceReport = resolveSingleInstanceReport;
            this.selectedReportNodeId = selectedReportNodeId;
            this.filterResolvedPlanNodes = (filterResolvedPlanNodes != null) ? filterResolvedPlanNodes : true;
        }
    }

    private ReportNodeTimeSeries getInMemoryReportNodeTimeSeries() {
        //Need to create a configuration with all time series details
        return new ReportNodeTimeSeries(new InMemoryCollectionFactory(new Properties()),
                // to build the report we only need a single time bucket and can flush only once all reports are ingested
                TimeSeriesCollectionsSettings.buildSingleResolutionSettings(Long.MAX_VALUE, 0), true);
    }

    /**
     * Populate an in memory timeseries and report node accessor with all data required to build a partial aggregated report tree
     * This aggregated tree will be filtered for the execution path of this single report node. If available we filter on the wrapping (nested) iteration
     * or simply on the selected node and its descendant
     *
     * @param aggregatedReport the aggregatedReport, it will be populated with the resolved path
     * @param selectedReportNodeIdStr the selected report node if for which we generate the partial report
     * @param reportNodeTimeSeries the inMemory report node time series to be populated
     * @param reportNodeAccessor   the inMemory report node accessor to be populated
     * @return the set of artefact hash part of the partial report
     */
    private Set<String> buildPartialReportNodeTimeSeries(AggregatedReport aggregatedReport, String selectedReportNodeIdStr, ReportNodeTimeSeries reportNodeTimeSeries, ReportNodeAccessor reportNodeAccessor) {
        ObjectId selectedReportNodeId = new ObjectId(selectedReportNodeIdStr);
        List<ReportNode> path = mainReportNodeAccessor.getReportNodePath(selectedReportNodeId);
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("Unable to determine the path of the selected node.");
        }
        Collections.reverse(path);
        ReportNode selectedReportNode = path.get(0);
        if (!selectedReportNode.getId().equals(selectedReportNodeId)) {
            throw new RuntimeException("Unable to determine the path of the selected node.");
        }
        // During ingestion, we store single report node per artefact hash in memory rather than saving all report nodes
        // There are only used to resolve single nodes when building the aggregated tree
        Map<String, ReportNode> singleReportNodes = new HashMap<>();
        // select the closest iteration node if any otherwise we fall back to the selected node
        ReportNode partialTreeRoot = path.stream().filter(this::isIterationNodeReport).findFirst().orElse(selectedReportNode);
        aggregatedReport.resolvedPartialPath = partialTreeRoot.getPath();
        ingestReportNodeRecursively(partialTreeRoot, reportNodeTimeSeries, singleReportNodes);
        reportNodeAccessor.save(singleReportNodes.values());
        reportNodeTimeSeries.flush();
        // build the set of artefact hash to be included in the report (re-ingested nodes + nodes of the path)
        Set<String> reportArtefactHashSet = new HashSet<>(singleReportNodes.keySet());
        reportArtefactHashSet.addAll(path.stream().map(ReportNode::getArtefactHash).collect(Collectors.toSet()));
        return reportArtefactHashSet;
    }

    private boolean isIterationNodeReport(ReportNode n) {
        AbstractArtefact resolvedArtefact = n.getResolvedArtefact();
        //All iteration work artefact are created with sequence artefact and names starting with "Iteration", we might change to a more robust implementation in the future
        return resolvedArtefact != null && resolvedArtefact.isWorkArtefact() && resolvedArtefact.getAttribute(AbstractOrganizableObject.NAME).startsWith("Iteration");
    }

    private void ingestReportNodeRecursively(ReportNode reportNode, ReportNodeTimeSeries reportNodeTimeSeries, Map<String, ReportNode> singleReportNodes) {
        reportNodeTimeSeries.ingestReportNode(reportNode);
        singleReportNodes.put(reportNode.getArtefactHash(), reportNode);
        this.mainReportNodeAccessor.getChildren(reportNode.getId()).forEachRemaining(child -> ingestReportNodeRecursively(child, reportNodeTimeSeries, singleReportNodes));
    }

    private AggregatedReportView recursivelyBuildAggregatedReportTree(ResolvedPlanNode resolvedPlanNode, AggregatedReportViewRequest request,
                                                                      ReportNodeTimeSeries reportNodesTimeSeries, ReportNodeAccessor reportNodeAccessor, Set<String> filteredArtefactHashSet) {
        String artefactHash = resolvedPlanNode.artefactHash;
        List<AggregatedReportView> children = resolvedPlanNodeAccessor.getByParentId(resolvedPlanNode.getId().toString())
                //filter nodes if filteredArtefactHashSet is provided and node is part of the set
                .filter(n -> filteredArtefactHashSet == null || filteredArtefactHashSet.contains(n.artefactHash))
                .map(n -> recursivelyBuildAggregatedReportTree(n, request, reportNodesTimeSeries, reportNodeAccessor, filteredArtefactHashSet))
                .collect(Collectors.toList());
        Map<String, Long> countByStatus = reportNodesTimeSeries.queryByExecutionIdAndArtefactHash(executionId, artefactHash, request.range);
        ReportNode singleInstanceReportNode = null;
        if (resolveSingleReport(request) && countByStatus.values().stream().reduce(0L, Long::sum) == 1) {
            singleInstanceReportNode = getSingleReportNodeInstance(reportNodeAccessor, executionId, artefactHash, request.range);
        }
        return new AggregatedReportView(resolvedPlanNode.artefact, artefactHash, countByStatus, children, resolvedPlanNode.parentSource, singleInstanceReportNode);
    }

    private boolean resolveSingleReport(AggregatedReportViewRequest request) {
        return (request.resolveSingleInstanceReport != null) ? request.resolveSingleInstanceReport : defaultResolveSingleInstanceReport;
    }

    private ReportNode getSingleReportNodeInstance(ReportNodeAccessor reportNodeAccessor, String executionId, String artefactHash, ReportNodeTimeSeries.Range range) {
        Long from = (range != null) ? range.from : null;
        Long to = (range != null) ? range.to : null;
        List<ReportNode> reports = reportNodeAccessor.getReportNodesByArtefactHash(executionId, artefactHash, from, to, 0, 2).collect(Collectors.toList());
        if (reports.size() == 1) {
            return reports.get(0);
        } else {
            String reportCountMsg = (reports.isEmpty()) ? "no" : "more than one";
            logger.error("Unexpected number of report nodes found. Found {} report for execution id {}, artefactHash: {} and range: {} to {}",
                    reportCountMsg, executionId, artefactHash, from, to);
            return null;
        }
    }

}
