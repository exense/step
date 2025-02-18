package step.core.artefacts.reports.aggregated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanNode;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanNodeAccessor;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AggregatedReportViewBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AggregatedReportViewBuilder.class);
    public static final String EXECUTION_REPORT_AGGREGATED_TREE_RESOLVE_SINGLE_INSTANCE = "execution.report.aggregated-tree.resolve-single-instance";

    private final String executionId;
    private final ExecutionAccessor executionAccessor;
    private final ResolvedPlanNodeAccessor resolvedPlanNodeAccessor;
    private final ReportNodeTimeSeries reportNodesTimeSeries;
    private final ReportNodeAccessor reportNodeAccessor;
    private final boolean defaultResolveSingleInstanceReport;

    public AggregatedReportViewBuilder(ExecutionEngineContext executionEngineContext, String executionId) {
        this.executionId = executionId;
        this.executionAccessor = executionEngineContext.getExecutionAccessor();
        this.resolvedPlanNodeAccessor = executionEngineContext.require(ResolvedPlanNodeAccessor.class);
        this.reportNodeAccessor = executionEngineContext.getReportNodeAccessor();
        this.defaultResolveSingleInstanceReport =  executionEngineContext.getConfiguration().getPropertyAsBoolean(EXECUTION_REPORT_AGGREGATED_TREE_RESOLVE_SINGLE_INSTANCE, true);
        reportNodesTimeSeries = executionEngineContext.require(ReportNodeTimeSeries.class);
    }

    public AggregatedReportView buildAggregatedReportView() {
        return buildAggregatedReportView(new AggregatedReportViewRequest(null, null));
    }

    public AggregatedReportView buildAggregatedReportView(AggregatedReportViewRequest request) {
        Objects.requireNonNull(request);
        Execution execution = executionAccessor.get(executionId);
        String aggregatedReportRoot = execution.getResolvedPlanRootNodeId();
        //Make sure the resolved Plan is available
        return Optional.ofNullable(execution.getResolvedPlanRootNodeId())
                .map(resolvedPlanNodeAccessor::get)
                .map(node -> recursivelyBuildAggregatedReportTree(node, request))
                .orElse(null);
    }

    public static class AggregatedReportViewRequest {
        public final ReportNodeTimeSeries.Range range;
        public final Boolean resolveSingleInstanceReport;

        @JsonCreator
        public AggregatedReportViewRequest(@JsonProperty("range") ReportNodeTimeSeries.Range range,
                                           @JsonProperty("resolveSingleInstanceReport") Boolean resolveSingleInstanceReport) {
            this.range = range;
            this.resolveSingleInstanceReport = resolveSingleInstanceReport;
        }
    }

    private AggregatedReportView recursivelyBuildAggregatedReportTree(ResolvedPlanNode resolvedPlanNode, AggregatedReportViewRequest request) {
        List<AggregatedReportView> children = resolvedPlanNodeAccessor.getByParentId(resolvedPlanNode.getId().toString())
                .map(n -> recursivelyBuildAggregatedReportTree(n, request))
                .collect(Collectors.toList());
        String artefactHash = resolvedPlanNode.artefactHash;
        Map<String, Long> countByStatus = reportNodesTimeSeries.queryByExecutionIdAndArtefactHash(executionId, artefactHash, request.range);
        ReportNode singleInstanceReportNode = null;
        if (resolveSingleReport(request) && countByStatus.values().stream().reduce(0L, Long::sum) == 1) {
            singleInstanceReportNode = getSingleReportNodeInstance(executionId, artefactHash, request.range);
        }
        return new AggregatedReportView(resolvedPlanNode.artefact, artefactHash, countByStatus, children, resolvedPlanNode.parentSource, singleInstanceReportNode);
    }

    private boolean resolveSingleReport(AggregatedReportViewRequest request) {
        return (request.resolveSingleInstanceReport != null) ? request.resolveSingleInstanceReport : defaultResolveSingleInstanceReport;
    }

    private ReportNode getSingleReportNodeInstance(String executionId, String artefactHash, ReportNodeTimeSeries.Range range) {
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
