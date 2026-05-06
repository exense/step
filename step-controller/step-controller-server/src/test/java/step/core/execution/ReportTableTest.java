package step.core.execution;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.Aggregator;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.Comparator;
import step.artefacts.Filter;
import step.artefacts.FilterType;
import step.artefacts.IfBlock;
import step.artefacts.PerformanceAssert;
import step.artefacts.handlers.AbstractFunctionHandlerTest;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.artefacts.handlers.functions.test.MyFunction;
import step.artefacts.reports.AssertReportNode;
import step.artefacts.reports.CallFunctionReportNode;
import step.artefacts.reports.PerformanceAssertReportNode;
import step.core.GlobalContextBuilder;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.ChildrenBlock;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.aggregated.AggregatedReport;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.artefacts.reports.aggregated.AggregatedReportViewBuilder;
import step.core.artefacts.reports.aggregated.AggregatedReportViewRequest;
import step.core.collections.Collection;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.table.EnrichedReportNode;
import step.core.execution.table.ReportNodeTableFilterFactory;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.PluginManager;
import step.engine.plugins.FunctionPlugin;
import step.framework.server.Session;
import step.framework.server.access.NoAuthorizationManager;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.framework.server.tables.service.*;
import step.functions.io.Output;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.plans.assertions.PerformanceAssertPlugin;
import step.threadpool.ThreadPoolPlugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.core.accessors.DefaultJacksonMapperProvider;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static step.planbuilder.BaseArtefacts.assertEqualArtefact;
import static step.planbuilder.BaseArtefacts.echo;
import static step.planbuilder.BaseArtefacts.set;

public class ReportTableTest {

    private ExecutionEngine engine;

    @Before
    public void before() {
        engine = new ExecutionEngine.Builder()
            .withPlugin(new BaseArtefactPlugin())
            .withPlugin(new ThreadPoolPlugin())
            .withPlugin(new FunctionPlugin())
            .withPlugin(AbstractFunctionHandlerTest.newMyFunctionTypePlugin())
            .withPlugin(new TokenForecastingExecutionPlugin())
            .withPlugin(new PerformanceAssertPlugin())
            .build();
    }

    @After
    public void after() {
        engine.close();
    }

    @Test
    public void testReportTable() throws TableServiceException {
        Plan plan = PlanBuilder.create()
            .startBlock(BaseArtefacts.for_(1, 10, 3))
            .add(echo("'test'"))
            .startBlock(BaseArtefacts.for_(1, 5))
            .add(set("key", "'value'"))
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
            .withTableFiltersFactory(new ReportNodeTableFilterFactory()).withResultListFactory(() -> new ArrayList<>() {
            }));
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

    @Test
    public void testEnrichCallKeywordReportWithAssertionErrors() throws TableServiceException, ClassNotFoundException, PluginManager.Builder.CircularDependencyException, InstantiationException, IllegalAccessException {
        MyFunction function = new MyFunction(input -> {
            Output<JsonObject> output = new Output<>();
            output.setPayload(Json.createObjectBuilder().add("Output1", "Value1").build());
            output.setMeasures(new ArrayList<>());
            output.setAttachments(new ArrayList<>());
            return output;
        });
        function.addAttribute(AbstractOrganizableObject.NAME, "MyFunction");

        //Plan with no assertion
        CallFunction callFunction = FunctionArtefacts.keyword(function.getAttribute(AbstractOrganizableObject.NAME));
        Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(callFunction).endBlock().build();
        plan.setFunctions(List.of(function));
        assertCallKeywordLeafReportNodes(plan, 0, false);

        // Assert nested directly under the keyword checks a wrong expected value → will fail
        step.artefacts.Assert anAssert = assertEqualArtefact("Output1", "wrongValue");
        callFunction.addChild(anAssert);
        plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(callFunction).endBlock().build();
        plan.setFunctions(List.of(function));
        assertCallKeywordLeafReportNodes(plan, 1, false);

        // Assert nested directly under the keyword checks a correct expected value → will pass and have no assert "on error" added to the call keyword report node
        anAssert.setExpected(new DynamicValue<>("Value1"));
        plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(callFunction).endBlock().build();
        plan.setFunctions(List.of(function));
        assertCallKeywordLeafReportNodes(plan, 0, false);

        //Test with performance assert - FAIL
        PerformanceAssert myFunctionPerformanceAssert = new PerformanceAssert(
            new ArrayList<>(List.of(new Filter(AbstractOrganizableObject.NAME, "MyFunction", FilterType.EQUALS))), // cannot be immutable
            Aggregator.COUNT, Comparator.EQUALS, 2L);
        ChildrenBlock childrenBlock = new ChildrenBlock();
        childrenBlock.addStep(myFunctionPerformanceAssert);
        callFunction.setAfter(childrenBlock);
        plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(callFunction).endBlock().build();
        plan.setFunctions(List.of(function));
        assertCallKeywordLeafReportNodes(plan, 1, true);


        //Test with performance assert - PASS
        myFunctionPerformanceAssert.setExpectedValue(new DynamicValue<>(1));
        plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(callFunction).endBlock().build();
        plan.setFunctions(List.of(function));
        assertCallKeywordLeafReportNodes(plan, 0, true);

        //now test with more depth
        step.artefacts.Assert anAssert2 = assertEqualArtefact("Output1", "wrongValue");
        IfBlock aTrue = new IfBlock("true");
        aTrue.addChild(anAssert2);
        callFunction.addChild(aTrue);
        plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(callFunction).endBlock().build();
        plan.setFunctions(List.of(function));
        assertCallKeywordLeafReportNodes(plan, 1, false);
    }

    @Test
    public void testEnrichedReportNodeSerialization() throws Exception {
        ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();

        CallFunctionReportNode delegate = new CallFunctionReportNode();
        delegate.setStatus(ReportNodeStatus.FAILED);

        AssertReportNode assertionError = new AssertReportNode();
        assertionError.setStatus(ReportNodeStatus.FAILED);

        EnrichedReportNode<CallFunctionReportNode> enriched =
                new EnrichedReportNode<>(delegate, List.of(assertionError));

        // direct serialization — exercises Serializer.serialize()
        JsonNode directJson = mapper.valueToTree(enriched);
        Assert.assertEquals(CallFunctionReportNode.class.getName(), directJson.get("_class").asText());
        Assert.assertEquals(ReportNodeStatus.FAILED.toString(), directJson.get("status").asText());
        JsonNode assertionNodes = directJson.get("assertionReportNodesOnError");
        Assert.assertNotNull(assertionNodes);
        Assert.assertEquals(1, assertionNodes.size());
        Assert.assertEquals(AssertReportNode.class.getName(), assertionNodes.get(0).get("_class").asText());

        // typed-container serialization — exercises Serializer.serializeWithType()
        String json = mapper.writerFor(new TypeReference<List<ReportNode>>() {}).writeValueAsString(List.of(enriched));
        JsonNode containerJson = mapper.readTree(json);
        JsonNode firstElement = containerJson.get(0);
        Assert.assertEquals(CallFunctionReportNode.class.getName(), firstElement.get("_class").asText());
        Assert.assertEquals(ReportNodeStatus.FAILED.toString(), firstElement.get("status").asText());
        JsonNode assertionNodesInContainer = firstElement.get("assertionReportNodesOnError");
        Assert.assertNotNull(assertionNodesInContainer);
        Assert.assertEquals(1, assertionNodesInContainer.size());
        Assert.assertEquals(AssertReportNode.class.getName(), assertionNodesInContainer.get(0).get("_class").asText());
    }

    private void assertCallKeywordLeafReportNodes(Plan plan, int expectedAssertionErrors, boolean isPerfAssert) throws TableServiceException, ClassNotFoundException, PluginManager.Builder.CircularDependencyException, InstantiationException, IllegalAccessException {
        PlanRunnerResult result = engine.execute(plan);
        ReportNodeStatus planResult = result.getResult();
        if (expectedAssertionErrors == 0) {
            Assert.assertEquals(ReportNodeStatus.PASSED, planResult);
        } else {
            Assert.assertEquals(ReportNodeStatus.FAILED, planResult);
        }

        // Wire up leafReports table the same way ExecutionPlugin does, using the engine's report collection
        Collection<ReportNode> reportsCollection = engine.getExecutionEngineContext().getReportAccessor().getCollectionDriver();
        TableRegistry tableRegistry = new TableRegistry();
        tableRegistry.register("leafReports", ExecutionPlugin.getLeafReportTable(GlobalContextBuilder.createGlobalContext(), reportsCollection));

        ReportNodesTableParameters params = new ReportNodesTableParameters();
        params.setEid(result.getExecutionId());
        params.setEnrichCallKeywordWithAssertionErrors(true);
        TableRequest tableRequest = new TableRequest();
        tableRequest.setFilters(List.of(new OQLFilter()));
        tableRequest.setTableParameters(params);

        TableService tableService = new TableService(tableRegistry, null, new NoAuthorizationManager());

        TableResponse<Object> response = tableService.request("leafReports", tableRequest, new Session<>());

        // Failed CallFunctionReportNodes are returned as EnrichedReportNode; passed ones are returned as-is
        ReportNode rawNode = response.getData().stream()
            .filter(n -> n instanceof CallFunctionReportNode || n instanceof EnrichedReportNode)
            .map(n -> (ReportNode) n)
            .findFirst()
            .orElseThrow();

        CallFunctionReportNode callFunctionReport;
        List<ReportNode> assertionErrors;
        if (rawNode instanceof EnrichedReportNode<?> enriched) {
            callFunctionReport = (CallFunctionReportNode) enriched.getDelegate();
            assertionErrors = enriched.getAssertionReportNodesOnError();
        } else {
            callFunctionReport = (CallFunctionReportNode) rawNode;
            assertionErrors = null;
        }

        if (expectedAssertionErrors == 0) {
            Assert.assertEquals(ReportNodeStatus.PASSED, callFunctionReport.getStatus());
            Assert.assertNull(assertionErrors);
        } else {
            Assert.assertEquals(ReportNodeStatus.FAILED, callFunctionReport.getStatus());
            Assert.assertNotNull(assertionErrors);
            Assert.assertEquals(expectedAssertionErrors, assertionErrors.size());
            if (isPerfAssert) {
                Assert.assertTrue(assertionErrors.getFirst() instanceof PerformanceAssertReportNode);
            } else {
                Assert.assertTrue(assertionErrors.getFirst() instanceof AssertReportNode);
            }
        }
    }
}
