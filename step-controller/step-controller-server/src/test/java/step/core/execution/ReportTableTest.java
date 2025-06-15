package step.core.execution;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.aggregated.AggregatedReport;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.artefacts.reports.aggregated.AggregatedReportViewBuilder;
import step.core.artefacts.reports.aggregated.AggregatedReportViewRequest;
import step.core.execution.table.ReportNodeTableFilterFactory;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.FunctionPlugin;
import step.framework.server.Session;
import step.framework.server.access.NoAuthorizationManager;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.framework.server.tables.service.*;
import step.planbuilder.BaseArtefacts;
import step.threadpool.ThreadPoolPlugin;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static step.planbuilder.BaseArtefacts.echo;
import static step.planbuilder.BaseArtefacts.set;

public class ReportTableTest {

    private ExecutionEngine engine;

    @Before
    public void before() {
        engine = new ExecutionEngine.Builder().withPlugin(new BaseArtefactPlugin()).withPlugin(new ThreadPoolPlugin())
                .withPlugin(new FunctionPlugin()).withPlugin(new TokenForecastingExecutionPlugin()).build();
    }

    @Test
    public void testReportTable() throws TableServiceException {
        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 10 ,3))
                .add(echo("'test'"))
                .startBlock(BaseArtefacts.for_(1, 5))
                .add(set("key","'value'"))
                .add(echo("'Echo 2'"))
                .add(echo("'Echo 3'"))
                .endBlock()
                .endBlock().build();
        PlanRunnerResult result = engine.execute(plan);

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView aggregatedReportView = aggregatedReportViewBuilder.buildAggregatedReportView();

        assertEquals(10, aggregatedReportView.children.get(0).countTotal());
        assertEquals(10, aggregatedReportView.children.get(1).countTotal());
        assertEquals(50, aggregatedReportView.children.get(1).children.get(0).countTotal());
        assertEquals(50, aggregatedReportView.children.get(1).children.get(1).countTotal());
        assertEquals("For: 1x: PASSED\n" +
                        " Echo: 10x: PASSED\n" +
                        " For: 10x: PASSED\n" +
                        "  Set: 50x: PASSED\n" +
                        "  Echo: 50x: PASSED\n" +
                        "  Echo: 50x: PASSED\n",
                aggregatedReportView.toString());

        // Test partial aggregated tree for single Echo (echo in the outer for loop)
        ReportNode reportNode = engine.getExecutionEngineContext().getReportNodeAccessor().getReportNodesByExecutionIDAndClass(result.getExecutionId(), "step.artefacts.reports.EchoReportNode").findFirst().orElseThrow(() -> new RuntimeException("No echo report aggregatedReportView found"));
        AggregatedReportViewRequest aggregatedReportViewRequest = new AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), true, null);
        AggregatedReport aggregatedReport = aggregatedReportViewBuilder.buildAggregatedReport(aggregatedReportViewRequest);

        assertEquals("For: *x\n" +
                        " Echo: 1x: PASSED > test\n" +
                        " For: 1x: PASSED\n" +
                        "  Set: 5x: PASSED\n" +
                        "  Echo: 5x: PASSED\n" +
                        "  Echo: 5x: PASSED\n",
                aggregatedReport.aggregatedReportView.toString());

        //Test table services
        TableRegistry tableRegistry = new TableRegistry();
        tableRegistry.register("reports", new Table<>(engine.getExecutionEngineContext().getReportAccessor().getCollectionDriver(), "execution-read", false)
                .withTableFiltersFactory(new ReportNodeTableFilterFactory()).withResultListFactory(()->new ArrayList<>(){}));
        TableService tableService = new TableService(tableRegistry, null, new NoAuthorizationManager());
        TableRequest tableRequest = new TableRequest();
        tableRequest.setFilters(List.of(new OQLFilter()));
        ReportNodesTableParameters reportNodesTableParameters = new ReportNodesTableParameters();
        reportNodesTableParameters.setEid(result.getExecutionId());
        tableRequest.setTableParameters(reportNodesTableParameters);
        // Request with no filter
        TableResponse<Object> reports = tableService.request("reports", tableRequest, new Session<>());

        Assert.assertEquals(232, reports.getRecordsFiltered());

        // Request with filter on the echo artefact Hash (outer loop)
        OQLFilter oqlFilter = new OQLFilter();
        oqlFilter.setOql("artefactHash = " + reportNode.getArtefactHash());
        tableRequest.setFilters(List.of(oqlFilter));
        reports = tableService.request("reports", tableRequest, new Session<>());
        Assert.assertEquals(10, reports.getRecordsFiltered());

        // Request with filter on the echo artefact Hash (inner loop) + selected aggregatedReport context
        oqlFilter = new OQLFilter();
        oqlFilter.setOql("artefactHash = " + reportNode.getArtefactHash() + " and path ~ \"^" + aggregatedReport.resolvedPartialPath + "\"");
        tableRequest.setFilters(List.of(oqlFilter));
        reports = tableService.request("reports", tableRequest, new Session<>());
        Assert.assertEquals(1, reports.getRecordsFiltered());

        //Test for node in inner loop
        ReportNode setReportNode = engine.getExecutionEngineContext().getReportNodeAccessor().getReportNodesByExecutionIDAndClass(result.getExecutionId(), "step.artefacts.reports.SetReportNode").findFirst().orElseThrow(() -> new RuntimeException("No echo report aggregatedReportView found"));
        oqlFilter = new OQLFilter();
        oqlFilter.setOql("artefactHash = " + setReportNode.getArtefactHash() + " and path ~ \"^" + aggregatedReport.resolvedPartialPath + "\"");
        tableRequest.setFilters(List.of(oqlFilter));
        reports = tableService.request("reports", tableRequest, new Session<>());
        Assert.assertEquals(5, reports.getRecordsFiltered());
    }
}
