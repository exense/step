package step.core.artefacts.reports.aggregated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import step.common.managedoperations.Operation;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.timeseries.bucket.Bucket;

import java.util.List;
import java.util.Map;

public class FlatAggregatedReportView extends AbstractAggregatedReportView {


    @JsonCreator
    public FlatAggregatedReportView(@JsonProperty("artefact") AbstractArtefact artefact, @JsonProperty("artefactHash") String artefactHash,
                                    @JsonProperty("countByStatus") Map<String, Long> countByStatus,
                                    @JsonProperty("countByErrorMessage") Map<String, Long> countByErrorMessage,
                                    @JsonProperty("countByContributingErrorMessage") Map<String, Long> countByChildrenErrorMessage,
                                    @JsonProperty("singleInstanceReportNode") ReportNode singleInstanceReportNode,
                                    @JsonProperty("bucketsByStatus") Map<String, Bucket> bucketsByStatus,
                                    @JsonProperty("currentOperations") List<Operation> currentOperations) {
        super(artefact, artefactHash, countByStatus, countByErrorMessage, countByChildrenErrorMessage, singleInstanceReportNode, bucketsByStatus, currentOperations);
    }

    public FlatAggregatedReportView(AggregatedReportView treeReportNode) {
        super(treeReportNode.artefact, treeReportNode.artefactHash, treeReportNode.countByStatus, treeReportNode.countByErrorMessage,
                treeReportNode.countByChildrenErrorMessage, treeReportNode.singleInstanceReportNode, treeReportNode.bucketsByStatus,
                treeReportNode.currentOperations);
    }
}
