package step.core.execution.table;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ReportNodesTableParameters;
import step.core.objectenricher.TriFunction;
import step.framework.server.Session;
import step.framework.server.tables.service.TableParameters;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ReportNodeTableEnricher implements TriFunction<ReportNode, Session<?>, TableParameters, ReportNode> {

    private final ReportNodeAccessor reportNodeAccessor;

    public ReportNodeTableEnricher(ReportNodeAccessor reportNodeAccessor) {
        this.reportNodeAccessor = reportNodeAccessor;
    }

    @Override
    public ReportNode apply(ReportNode reportNode, Session<?> session, TableParameters tableParameters) {
        if (tableParameters instanceof ReportNodesTableParameters reportNodesTableParameters) {
            if (shouldEnrich(reportNode, reportNodesTableParameters)) {
                List<ReportNode> contributingErrors = collectContributingErrors(reportNode, reportNodesTableParameters);
                return new EnrichedReportNode<>(reportNode, contributingErrors);
            }
        }
        return reportNode;
    }

    private static boolean shouldEnrich(ReportNode reportNode, ReportNodesTableParameters tableParameters) {
        return tableParameters.isEnrichWithContributingErrors()
            && (mayHaveChildren(reportNode))
            && ((ReportNodeStatus.FAILED.equals(reportNode.getStatus())) || (ReportNodeStatus.TECHNICAL_ERROR.equals(reportNode.getStatus())));
    }

    /**
     * Determine whether this type of nodes can have children based on the underlying resolved artefact.
     * Returns {@code true} (safe default matching {@link Artefact#block()}'s default) when the resolved
     * artefact or its annotation cannot be determined at this point.
     *
     * @param reportNode the report node to evaluate
     * @return whether it may have children report nodes
     */
    private static boolean mayHaveChildren(ReportNode reportNode) {
        AbstractArtefact resolvedArtefact = reportNode.getResolvedArtefact();
        if (resolvedArtefact == null) {
            return true;
        }
        Artefact artefactAnnotation = resolvedArtefact.getClass().getAnnotation(Artefact.class);
        return (artefactAnnotation == null || artefactAnnotation.block());
    }

    private List<ReportNode> collectContributingErrors(ReportNode root, ReportNodesTableParameters tableParameters) {
        try (Stream<ReportNode> reportNodesWithContributingErrors = reportNodeAccessor.getReportNodesWithContributingErrors(
            root.getExecutionID(), root.getId().toHexString(),
            0, tableParameters.getEnrichWithContributingErrorsLimit())) {
            return reportNodesWithContributingErrors.collect(Collectors.toList());
        }
    }
}
