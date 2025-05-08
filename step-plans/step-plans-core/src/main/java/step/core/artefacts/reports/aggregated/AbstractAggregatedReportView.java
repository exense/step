package step.core.artefacts.reports.aggregated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import step.common.managedoperations.Operation;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.timeseries.bucket.Bucket;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractAggregatedReportView {

    public final AbstractArtefact artefact;
    public final String artefactHash;
    public final Map<String, Long> countByStatus;
    public final ReportNode singleInstanceReportNode;
    public final Map<String, Bucket> bucketsByStatus;
    public final List<Operation> currentOperations;

    @JsonCreator
    public AbstractAggregatedReportView(@JsonProperty("artefact") AbstractArtefact artefact, @JsonProperty("artefactHash") String artefactHash,
                                        @JsonProperty("countByStatus") Map<String, Long> countByStatus,
                                        @JsonProperty("singleInstanceReportNode") ReportNode singleInstanceReportNode,
                                        @JsonProperty("bucketsByStatus") Map<String, Bucket> bucketsByStatus,
                                        @JsonProperty("currentOperations") List<Operation> currentOperations) {
        this.artefact = artefact;
        this.artefactHash = artefactHash;
        this.countByStatus = countByStatus;
        this.singleInstanceReportNode = singleInstanceReportNode;
        this.bucketsByStatus = bucketsByStatus;
        this.currentOperations = currentOperations;
    }

    public long countTotal() {
        return countByStatus.values().stream().reduce(Long::sum).orElse(0L);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        //String name = node.artefact.getClass().getSimpleName();
        String name = artefact.getAttribute(AbstractOrganizableObject.NAME);
        String indentation = " ";
        stringBuilder.append(indentation);
        stringBuilder.append(name).append(": ").append(getStatusCountDetails(this));
        //Print report for single instance
        if (singleInstanceReportNode != null) {
            String reportAsString = singleInstanceReportNode.getReportAsString();
            if (reportAsString != null && !reportAsString.isBlank()) {
                stringBuilder.append(" > ").append(reportAsString);
            }
        }
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }


    public StringBuffer getStatusCountDetails(AbstractAggregatedReportView node) {
        StringBuffer stringBuffer = new StringBuffer();
        long totalCount = node.countTotal();
        stringBuffer.append(totalCount).append("x");
        if (totalCount > 0) {
            stringBuffer.append(": ");
            if (node.countByStatus.size() == 1) {
                stringBuffer.append(node.countByStatus.keySet().stream().findFirst().orElseThrow(() -> new RuntimeException("No statuses found")));
            } else {
                stringBuffer.append(node.countByStatus.entrySet().stream().map(entry -> entry.getValue() + " " + entry.getKey()).collect(Collectors.joining(", ")));
            }
        }
        return stringBuffer;
    }
}
