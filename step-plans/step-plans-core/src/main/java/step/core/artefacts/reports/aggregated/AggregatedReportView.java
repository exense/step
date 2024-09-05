package step.core.artefacts.reports.aggregated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import step.core.artefacts.AbstractArtefact;

import java.util.List;
import java.util.Map;

public class AggregatedReportView {

    public final AbstractArtefact artefact;
    public final String artefactHash;
    public final Map<String, Long> countByStatus;
    public final List<AggregatedReportView> children;

    @JsonCreator
    public AggregatedReportView(@JsonProperty("artefact") AbstractArtefact artefact, @JsonProperty("artefactHash") String artefactHash,
                                @JsonProperty("countByStatus") Map<String, Long> countByStatus, @JsonProperty("children") List<AggregatedReportView> children) {
        this.artefact = artefact;
        this.artefactHash = artefactHash;
        this.countByStatus = countByStatus;
        this.children = children;
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
        stringBuilder.append(indentation).append(name).append(": ").append(node.countTotal()).append("x\n");
        node.children.forEach(c -> recursiveToString(c, level + 1, stringBuilder));
    }
}
