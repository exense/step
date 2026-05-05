package step.core.execution;

import org.apache.poi.ss.formula.functions.T;
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
    public static final String ASSERTION_REPORT_NODES_ON_ERROR = "assertionReportNodesOnError";

    private final Collection<ReportNode> reportsCollection;

    public ReportNodeTableEnricher(Collection<ReportNode> reportsCollection) {
        this.reportsCollection = reportsCollection;
    }

    @Override
    public ReportNode apply(ReportNode reportNode, Session<?> session, TableParameters tableParameters) {
        if (shouldEnrichReportNodeTable(reportNode, tableParameters)) {
            enrichFailedCallFunctionReportWithAssertionErrors((CallFunctionReportNode) reportNode);
        }
        return reportNode;
    }

    private static boolean shouldEnrichReportNodeTable(ReportNode reportNode, TableParameters tableParameters) {
        return (tableParameters instanceof ReportNodesTableParameters)
            && ((ReportNodesTableParameters) tableParameters).isEnrichCallKeywordWithAssertionErrors()
            && (reportNode instanceof CallFunctionReportNode) && ReportNodeStatus.FAILED.equals(reportNode.getStatus());
    }

    void enrichFailedCallFunctionReportWithAssertionErrors(CallFunctionReportNode callFunctionReportNode) {
        // Because we want to support Asserts not located as direct children (i.e. inside an intermediate IF block...),
        // we fetch all FAILED children except nested call keywords recursively
        // We still apply a max number of children and a max depth
        enrichFailedCallFunctionReportWithAssertionErrors(callFunctionReportNode, callFunctionReportNode, 0);
    }

    void enrichFailedCallFunctionReportWithAssertionErrors(CallFunctionReportNode callFunctionReportNode, ReportNode currentReportNode, int depth) {
        if (depth > MAX_CALL_FUNCTION_REPORT_RECURSION) {
            logger.warn("Depth of children limit ({}) has been reached while searching for assertion errors. Execution ID: {}, Call Keyword report node id: {}",
                MAX_CALL_FUNCTION_REPORT_RECURSION, callFunctionReportNode.getExecutionID(), callFunctionReportNode.getId().toHexString());
            return;
        }
        AtomicInteger recordCounter = new AtomicInteger(0);
        And filter = Filters.and(List.of(Filters.equals("parentID", currentReportNode.getId()),
            Filters.equals("status", ReportNodeStatus.FAILED.toString()),
            Filters.not(Filters.equals("_class", "step.artefacts.reports.CallFunctionReportNode"))));
        try (Stream<ReportNode> reportNodeStream = reportsCollection.findLazy(filter, null,
            0, MAX_CALL_FUNCTION_REPORT_CHILDREN + 1, 0)) {
            reportNodeStream.filter(r -> recordCounter.getAndIncrement() < MAX_CALL_FUNCTION_REPORT_CHILDREN)
                .forEach(reportNode -> {
                    if (reportNode instanceof PerformanceAssertReportNode || reportNode instanceof AssertReportNode) {
                        callFunctionReportNode.computeCustomFieldIfAbsent(ASSERTION_REPORT_NODES_ON_ERROR, k -> new ArrayList<ReportNode>())
                            .add(reportNode);
                    } else {
                        //recursively process the current node
                        enrichFailedCallFunctionReportWithAssertionErrors(callFunctionReportNode, reportNode, depth + 1);
                    }
                });
        }
        if (recordCounter.get() >= MAX_CALL_FUNCTION_REPORT_CHILDREN) {
            logger.warn("Number of children limit ({}) has been reached while searching for assertion errors. Execution ID: {}, Call Keyword report node id: {}",
                MAX_CALL_FUNCTION_REPORT_CHILDREN, callFunctionReportNode.getExecutionID(), callFunctionReportNode.getId().toHexString());
        }
    }
}
