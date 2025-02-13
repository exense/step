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

import java.io.IOException;
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

    public AggregatedReportView buildAggregatedReportView() {
        return buildAggregatedReportView(new AggregatedReportViewRequest(null, null, null));
    }

    public AggregatedReportView buildAggregatedReportView(AggregatedReportViewRequest request) {
        Objects.requireNonNull(request);
        Execution execution = executionAccessor.get(executionId);
        //Make sure the resolved Plan is available
        ResolvedPlanNode rootResolvedPlanNode = Optional.ofNullable(execution.getResolvedPlanRootNodeId()).map(resolvedPlanNodeAccessor::get).orElse(null);
        if (rootResolvedPlanNode == null) {
            return null;
        } else if (request.selectedReportNodeId == null) {
            // Generate complete aggregated report tree
            return recursivelyBuildAggregatedReportTree(rootResolvedPlanNode, request, mainReportNodesTimeSeries, mainReportNodeAccessor);
        } else {
            // a node is selected to generate a partial aggregated report
            try (ReportNodeTimeSeries localReportNodesTimeSeries = getInMemoryReportNodeTimeSeries()) {
                InMemoryReportNodeAccessor inMemoryReportNodeAccessor = new InMemoryReportNodeAccessor();
                buildPartialReportNodeTimeSeries(request.selectedReportNodeId, localReportNodesTimeSeries, inMemoryReportNodeAccessor);
                return recursivelyBuildAggregatedReportTree(rootResolvedPlanNode, request, localReportNodesTimeSeries, inMemoryReportNodeAccessor);
            } catch (IOException e) {
                //Handle auto-closable exception, the aggregated report view was created in all cases.
                logger.error("Unable to close the local report node time series", e);
                return null;
            }
        }
    }

    public static class AggregatedReportViewRequest {
        public final ReportNodeTimeSeries.Range range;
        public final Boolean resolveSingleInstanceReport;
        public final String selectedReportNodeId;

        @JsonCreator
        public AggregatedReportViewRequest(@JsonProperty("range") ReportNodeTimeSeries.Range range,
                                           @JsonProperty("resolveSingleInstanceReport") Boolean resolveSingleInstanceReport,
                                           @JsonProperty("selectedReportNodeId") String selectedReportNodeId) {
            this.range = range;
            this.resolveSingleInstanceReport = resolveSingleInstanceReport;
            this.selectedReportNodeId = selectedReportNodeId;
        }
    }

    private ReportNodeTimeSeries getInMemoryReportNodeTimeSeries() {
        //Need to create a configuration with all time series details
        return new ReportNodeTimeSeries(new InMemoryCollectionFactory(new Properties()),
                // to build the report we only need a single time bucket and can flush only once all reports are ingested
                TimeSeriesCollectionsSettings.buildSingleResolutionSettings(Long.MAX_VALUE, 0));
    }

    /**
     * Create an in memory timeseries and report node accessor containing all data required to build a partial aggregated report tree
     * This aggregated tree will be filtered for the execution path of this single report node. If available we filter on the wrapping (nested) iteration
     * or simply on the selected node and its descendant
     *
     * @param selectedReportNodeIdStr the id of the selected report node
     * @param reportNodeTimeSeries the inMemory report node time series to be populated
     * @param reportNodeAccessor the inMemory report node accessor to be populated
     */
    private void buildPartialReportNodeTimeSeries(String selectedReportNodeIdStr, ReportNodeTimeSeries reportNodeTimeSeries, ReportNodeAccessor reportNodeAccessor) {
        ObjectId selectedReportNodeId = new ObjectId(selectedReportNodeIdStr);
        List<ReportNode> path = mainReportNodeAccessor.getReportNodePath(selectedReportNodeId);
        // During ingestion, we store single report node per artefact hash in memory rather than saving all report nodes
        // There are only used to resolve single nodes when building the aggregated tree
        Map<String, ReportNode> singleReportNodes = new HashMap<>();
        path.stream().filter(n -> n.getId().equals(selectedReportNodeId) || isIterationNodeReport(n)).findFirst()
                .ifPresent(n -> ingestReportNodeRecursively(n, reportNodeTimeSeries, singleReportNodes));
        reportNodeAccessor.save(singleReportNodes.values());
        reportNodeTimeSeries.flush();
    }

    private boolean isIterationNodeReport(ReportNode n) {
        AbstractArtefact resolvedArtefact = n.getResolvedArtefact();
        //TODO add a property to artefact for such "iteration" artefact"
        return resolvedArtefact != null && resolvedArtefact.isWorkArtefact() && resolvedArtefact.getAttribute(AbstractOrganizableObject.NAME).startsWith("Iteration");
    }

    private void ingestReportNodeRecursively(ReportNode reportNode, ReportNodeTimeSeries reportNodeTimeSeries, Map<String, ReportNode> singleReportNodes) {
        reportNodeTimeSeries.ingestReportNode(reportNode);
        singleReportNodes.put(reportNode.getArtefactHash(), reportNode);
        this.mainReportNodeAccessor.getChildren(reportNode.getId()).forEachRemaining(child -> ingestReportNodeRecursively(child, reportNodeTimeSeries, singleReportNodes));
    }

    private AggregatedReportView recursivelyBuildAggregatedReportTree(ResolvedPlanNode resolvedPlanNode, AggregatedReportViewRequest request,
                                                                      ReportNodeTimeSeries reportNodesTimeSeries, ReportNodeAccessor reportNodeAccessor) {
        List<AggregatedReportView> children = resolvedPlanNodeAccessor.getByParentId(resolvedPlanNode.getId().toString())
                .map(n -> recursivelyBuildAggregatedReportTree(n, request, reportNodesTimeSeries, reportNodeAccessor))
                .collect(Collectors.toList());
        String artefactHash = resolvedPlanNode.artefactHash;
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
