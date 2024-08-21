package step.core.artefacts.reports.aggregated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ParentSource;

import java.util.List;
import java.util.Map;

public class AggregatedReportView {

    public final AbstractArtefact artefact;
    public final String artefactHash;
    public final Map<String, Long> countByStatus;
    public final List<AggregatedReportView> children;
    public final ParentSource parentSource;

    @JsonCreator
    public AggregatedReportView(@JsonProperty("artefact") AbstractArtefact artefact, @JsonProperty("artefactHash") String artefactHash,
                                @JsonProperty("countByStatus") Map<String, Long> countByStatus, @JsonProperty("children") List<AggregatedReportView> children,
                                @JsonProperty("parentSource") ParentSource parentSource) {
        this.artefact = artefact;
        this.artefactHash = artefactHash;
        this.countByStatus = countByStatus;
        this.children = children;
        this.parentSource = parentSource;
    }

    public long countTotal() {
        return countByStatus.values().stream().reduce(Long::sum).orElse(0L);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        recursiveToString(this, 0, stringBuilder);
        return stringBuilder.toString();
    }

    private static void recursiveToString(AggregatedReportView node, int level, StringBuilder stringBuilder) {
        String name = node.artefact.getClass().getSimpleName();
        String indentation = new String(new char[level]).replace("\0", " ");
        stringBuilder.append(indentation);
        stringBuilder.append(name).append(": ").append(node.countTotal()).append("x\n");
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
