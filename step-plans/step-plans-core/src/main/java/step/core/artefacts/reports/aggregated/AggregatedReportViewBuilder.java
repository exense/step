package step.core.artefacts.reports.aggregated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.function.Function;
import java.util.stream.Collectors;

public class AggregatedReportViewBuilder {

    private final String executionId;
    private final ExecutionAccessor executionAccessor;
    private final ResolvedPlanNodeAccessor resolvedPlanNodeAccessor;
    private final ReportNodeTimeSeries reportNodesTimeSeries;
    private final ReportNodeAccessor reportNodeAccessor;

    public AggregatedReportViewBuilder(ExecutionEngineContext executionEngineContext, String executionId) {
        this.executionId = executionId;
        this.executionAccessor = executionEngineContext.getExecutionAccessor();
        this.resolvedPlanNodeAccessor = executionEngineContext.require(ResolvedPlanNodeAccessor.class);
        this.reportNodeAccessor = executionEngineContext.getReportNodeAccessor();
        reportNodesTimeSeries = executionEngineContext.require(ReportNodeTimeSeries.class);
    }

    public AggregatedReportView buildAggregatedReportView() {
        return buildAggregatedReportView(new AggregatedReportViewRequest(null));
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

        @JsonCreator
        public AggregatedReportViewRequest(@JsonProperty("range") ReportNodeTimeSeries.Range range) {
            this.range = range;
        }
    }

    private AggregatedReportView recursivelyBuildAggregatedReportTree(ResolvedPlanNode resolvedPlanNode, AggregatedReportViewRequest request) {
        List<AggregatedReportView> children = resolvedPlanNodeAccessor.getByParentId(resolvedPlanNode.getId().toString())
                .map(n -> recursivelyBuildAggregatedReportTree(n, request))
                .collect(Collectors.toList());
        String artefactHash = resolvedPlanNode.artefactHash;
        Map<String, Long> countByStatus = reportNodesTimeSeries.queryByExecutionIdAndArtefactHash(executionId, artefactHash, request.range);
        ReportNode singleInstanceReportNode = null;
        if (countByStatus.values().stream().reduce(0L, Long::sum) == 1) {
            singleInstanceReportNode = getSingleReportNodeInstance(executionId, artefactHash, request.range);
        }
        return new AggregatedReportView(resolvedPlanNode.artefact, artefactHash, countByStatus, children, resolvedPlanNode.parentSource, singleInstanceReportNode);
    }

    private ReportNode getSingleReportNodeInstance(String executionId, String artefactHash, ReportNodeTimeSeries.Range range) {
        return reportNodeAccessor.getReportNodesByArtefactHash(executionId, artefactHash, (range != null) ? range.from : null, (range != null) ? range.to : null, 0, 1).findFirst().orElse(null);
    }

}
