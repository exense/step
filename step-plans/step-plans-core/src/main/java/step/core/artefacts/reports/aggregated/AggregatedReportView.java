package step.core.artefacts.reports.aggregated;

import step.core.artefacts.AbstractArtefact;

import java.util.List;
import java.util.Map;

public class AggregatedReportView {

    public final AbstractArtefact artefact;
    public final String artefactHash;
    public final Map<String, Long> countByStatus;
    public final List<AggregatedReportView> children;

    public AggregatedReportView(AbstractArtefact artefact, String artefactHash, Map<String, Long> countByStatus, List<AggregatedReportView> children) {
        this.artefact = artefact;
        this.artefactHash = artefactHash;
        this.countByStatus = countByStatus;
        this.children = children;
    }

    public long countTotal() {
        return countByStatus.values().stream().reduce(Long::sum).get();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        recursiveToString(this, 0, stringBuilder);
        return stringBuilder.toString();
    }

    private void recursiveToString(AggregatedReportView node, int level, StringBuilder stringBuilder) {
        String name = node.artefact.getClass().getSimpleName();
        String indentation = new String(new char[level]).replace("\0", " ");
        stringBuilder.append(indentation).append(name).append(": ").append(countTotal()).append("x\n");
        node.children.forEach(c -> recursiveToString(c, level + 1, stringBuilder));
    }
}
