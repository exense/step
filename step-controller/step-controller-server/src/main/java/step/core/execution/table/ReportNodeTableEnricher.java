package step.core.execution.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.reports.AssertReportNode;
import step.artefacts.reports.CallFunctionReportNode;
import step.artefacts.reports.PerformanceAssertReportNode;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.filters.And;
import step.core.execution.ReportNodesTableParameters;
import step.core.objectenricher.TriFunction;
import step.framework.server.Session;
import step.framework.server.tables.service.TableParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


public class ReportNodeTableEnricher implements TriFunction<ReportNode, Session<?>, TableParameters, ReportNode> {

    private static final Logger logger = LoggerFactory.getLogger(ReportNodeTableEnricher.class);

    private static final int MAX_CALL_FUNCTION_REPORT_CHILDREN = 1000;
    public static final int MAX_CALL_FUNCTION_REPORT_RECURSION = 10;

    private final Collection<ReportNode> reportsCollection;

    public ReportNodeTableEnricher(Collection<ReportNode> reportsCollection) {
        this.reportsCollection = reportsCollection;
    }

    @Override
    public ReportNode apply(ReportNode reportNode, Session<?> session, TableParameters tableParameters) {
        if (shouldEnrich(reportNode, tableParameters)) {
            List<ReportNode> assertionErrors = new ArrayList<>();
            collectAssertionErrors((CallFunctionReportNode) reportNode, reportNode, 0, assertionErrors);
            return new EnrichedReportNode<>((CallFunctionReportNode) reportNode, assertionErrors);
        }
        return reportNode;
    }

    private static boolean shouldEnrich(ReportNode reportNode, TableParameters tableParameters) {
        return (tableParameters instanceof ReportNodesTableParameters)
            && ((ReportNodesTableParameters) tableParameters).isEnrichCallKeywordWithAssertionErrors()
            && (reportNode instanceof CallFunctionReportNode)
            && ReportNodeStatus.FAILED.equals(reportNode.getStatus());
    }

    private void collectAssertionErrors(CallFunctionReportNode root, ReportNode current, int depth, List<ReportNode> collected) {
        if (depth > MAX_CALL_FUNCTION_REPORT_RECURSION) {
            logger.warn("Depth of children limit ({}) has been reached while searching for assertion errors. Execution ID: {}, Call Keyword report node id: {}",
                MAX_CALL_FUNCTION_REPORT_RECURSION, root.getExecutionID(), root.getId().toHexString());
            return;
        }
        AtomicInteger recordCounter = new AtomicInteger(0);
        And filter = Filters.and(List.of(
            Filters.equals("parentID", current.getId()),
            Filters.equals("status", ReportNodeStatus.FAILED.toString()),
            Filters.not(Filters.equals("_class", "step.artefacts.reports.CallFunctionReportNode"))));
        try (Stream<ReportNode> reportNodeStream = reportsCollection.findLazy(filter, null,
            0, MAX_CALL_FUNCTION_REPORT_CHILDREN + 1, 0)) {
            reportNodeStream
                .filter(r -> recordCounter.getAndIncrement() < MAX_CALL_FUNCTION_REPORT_CHILDREN)
                .forEach(child -> {
                    if (child instanceof PerformanceAssertReportNode || child instanceof AssertReportNode) {
                        collected.add(child);
                    } else {
                        collectAssertionErrors(root, child, depth + 1, collected);
                    }
                });
        }
        if (recordCounter.get() >= MAX_CALL_FUNCTION_REPORT_CHILDREN) {
            logger.warn("Number of children limit ({}) has been reached while searching for assertion errors. Execution ID: {}, Call Keyword report node id: {}",
                MAX_CALL_FUNCTION_REPORT_CHILDREN, root.getExecutionID(), root.getId().toHexString());
        }
    }
}
