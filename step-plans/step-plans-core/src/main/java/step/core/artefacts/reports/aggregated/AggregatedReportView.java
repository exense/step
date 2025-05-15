package step.core.artefacts.reports.aggregated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import step.common.managedoperations.Operation;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ParentSource;
import step.core.artefacts.reports.ReportNode;
import step.core.timeseries.bucket.Bucket;

import java.util.List;
import java.util.Map;

public class AggregatedReportView extends AbstractAggregatedReportView {

    public final List<AggregatedReportView> children;
    public final ParentSource parentSource;

    @JsonCreator
    public AggregatedReportView(@JsonProperty("artefact") AbstractArtefact artefact, @JsonProperty("artefactHash") String artefactHash,
                                @JsonProperty("countByStatus") Map<String, Long> countByStatus, @JsonProperty("children") List<AggregatedReportView> children,
                                @JsonProperty("parentSource") ParentSource parentSource,
                                @JsonProperty("singleInstanceReportNode") ReportNode singleInstanceReportNode,
                                @JsonProperty("bucketsByStatus") Map<String, Bucket> bucketsByStatus,
                                @JsonProperty("currentOperations") List<Operation> currentOperations) {
        super(artefact, artefactHash, countByStatus, singleInstanceReportNode, bucketsByStatus, currentOperations);
        this.children = children;
        this.parentSource = parentSource;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        recursiveToString(this, 0, stringBuilder);
        return stringBuilder.toString();
    }

    private void recursiveToString(AggregatedReportView node, int level, StringBuilder stringBuilder) {
        //String name = node.artefact.getClass().getSimpleName();
        String name = node.artefact.getAttribute(AbstractOrganizableObject.NAME);
        String indentation = " ".repeat(level);
        stringBuilder.append(indentation);
        stringBuilder.append(name).append(": ").append(getStatusCountDetails(node));
        //Print report for single instance
        if (node.singleInstanceReportNode != null) {
            String reportAsString = node.singleInstanceReportNode.getReportAsString();
            if (reportAsString != null && !reportAsString.isBlank()) {
                stringBuilder.append(" > ").append(reportAsString);
            }
        }
        stringBuilder.append("\n");
        //Process children considering parent source
        ParentSource previousParentSource = null;
        for (AggregatedReportView childReportView: node.children) {
            int newLevel = level + 1;
            ParentSource parentSource = childReportView.parentSource;
            //print [ParentSource] once per source type if required
            if (!parentSource.equals(previousParentSource) && parentSource.printSource) {
                previousParentSource = parentSource;
                String sourceIndentation = new String(new char[newLevel]).replace("\0", " ");
                stringBuilder.append(sourceIndentation);
                stringBuilder.append("[").append(parentSource.name()).append("]\n");
            }
            //If parent source was printed, indent children
            newLevel += (parentSource.printSource) ? 1 : 0;
            recursiveToString(childReportView, newLevel, stringBuilder);
        }
    }

}
