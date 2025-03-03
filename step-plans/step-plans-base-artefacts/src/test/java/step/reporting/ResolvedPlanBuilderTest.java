package step.reporting;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallPlan;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.artefacts.reports.aggregated.AggregatedReportViewBuilder;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.FunctionPlugin;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.threadpool.ThreadPoolPlugin;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static step.planbuilder.BaseArtefacts.*;

public class ResolvedPlanBuilderTest {

    protected static final Logger logger = LoggerFactory.getLogger(ResolvedPlanBuilderTest.class);

    private ExecutionEngine engine;

    @Before
    public void before() {
        engine = new ExecutionEngine.Builder()
                .withPlugin(new BaseArtefactPlugin())
                .withPlugin(new ThreadPoolPlugin())
                .withPlugin(new FunctionPlugin())
                .withPlugin(new TokenForecastingExecutionPlugin()).build();
    }

    @After
    public void after() {
        engine.close();
    }

    @Test
    public void simpleTest() throws IOException {
        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.threadGroup(2, 2))
                .startBlock(FunctionArtefacts.session())
                .add(echo("'Echo'"))
                .endBlock()
                .endBlock().build();
        PlanRunnerResult result = engine.execute(plan);
        result.printTree();

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();

        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("ThreadGroup: 1x: PASSED\n" +
                        " Session: 4x: PASSED\n" +
                        "  Echo: 4x: PASSED\n",
                node.toString());

        // Test partial aggregated tree
        ReportNode reportNode = engine.getExecutionEngineContext().getReportNodeAccessor().getReportNodesByExecutionIDAndClass(result.getExecutionId(), "step.artefacts.reports.EchoReportNode").findFirst().orElseThrow(() -> new RuntimeException("No echo report node found"));
        AggregatedReportViewBuilder.AggregatedReportViewRequest aggregatedReportViewRequest = new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), false);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Partial aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("ThreadGroup: 0x\n" +
                        " Session: 1x: PASSED\n" +
                        "  Echo: 1x: PASSED > Echo\n",
                node.toString());
    }

    @Test
    public void simpleTestWithErrors() throws IOException {
        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.threadGroup(1, 4))
                .startBlock(FunctionArtefacts.session())
                .add(echo("'Echo gcounter ' + gcounter"))
                .add(check("if (gcounter % 3 == 0) { return true} else if (gcounter % 3 == 1) { return false} else if (gcounter % 3 == 2) {return var}"))
                .endBlock()
                .endBlock().build();
        PlanRunnerResult result = engine.execute(plan);
        result.printTree();

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();

        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("ThreadGroup: 1x: TECHNICAL_ERROR\n" +
                        " Session: 4x: 1 TECHNICAL_ERROR, 2 FAILED, 1 PASSED\n" +
                        "  Echo: 4x: PASSED\n" +
                        "  Check: 4x: 1 TECHNICAL_ERROR, 2 FAILED, 1 PASSED\n",
                node.toString());

        // Test partial aggregated tree
        ReportNode reportNode = engine.getExecutionEngineContext().getReportNodeAccessor().getReportNodesByExecutionIDAndClass(result.getExecutionId(), "step.artefacts.reports.EchoReportNode").findFirst().orElseThrow(() -> new RuntimeException("No echo report node found"));
        AggregatedReportViewBuilder.AggregatedReportViewRequest aggregatedReportViewRequest = new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), false);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Partial aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("ThreadGroup: 0x\n" +
                        " Session: 1x: FAILED\n" +
                        "  Echo: 1x: PASSED > Echo gcounter 1\n" +
                        "  Check: 1x: FAILED > if (gcounter % 3 == 0) { return true} else if (gcounter % 3 == 1) { return false} else if (gcounter % 3 == 2) {return var}\n",
                node.toString());

        // Test partial aggregated tree, filtering also the aggregated report --> in this case the report should be the same
        aggregatedReportViewRequest = new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), null);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Partial aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("ThreadGroup: 0x\n" +
                        " Session: 1x: FAILED\n" +
                        "  Echo: 1x: PASSED > Echo gcounter 1\n" +
                        "  Check: 1x: FAILED > if (gcounter % 3 == 0) { return true} else if (gcounter % 3 == 1) { return false} else if (gcounter % 3 == 2) {return var}\n",
                node.toString());
    }

    @Test
    public void get() throws IOException, InterruptedException {
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
        result.printTree();

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();

        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());
        assertEquals(10, node.children.get(0).countTotal());
        assertEquals(10, node.children.get(1).countTotal());
        assertEquals(50, node.children.get(1).children.get(0).countTotal());
        assertEquals(50, node.children.get(1).children.get(1).countTotal());
        assertEquals("For: 1x: PASSED\n" +
                        " Echo: 10x: PASSED\n" +
                        " For: 10x: PASSED\n" +
                        "  Set: 50x: PASSED\n" +
                        "  Echo: 50x: PASSED\n" +
                        "  Echo: 50x: PASSED\n",
                node.toString());

        // Test partial aggregated tree for single Set
        ReportNode reportNode = engine.getExecutionEngineContext().getReportNodeAccessor().getReportNodesByExecutionIDAndClass(result.getExecutionId(), "step.artefacts.reports.SetReportNode").findFirst().orElseThrow(() -> new RuntimeException("No echo report node found"));
        AggregatedReportViewBuilder.AggregatedReportViewRequest aggregatedReportViewRequest = new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), true);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Partial aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("For: 0x\n" +
                        " For: 0x\n" +
                        "  Set: 1x: PASSED > key = value\n" +
                        "  Echo: 1x: PASSED > Echo 2\n" +
                        "  Echo: 1x: PASSED > Echo 3\n",
                node.toString());
    }

    @Test
    public void planWithSimpleCallPlan() throws IOException, InterruptedException {
        Plan subPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(echo("'in sub plan'"))
                .endBlock().build();

        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 1))
                .startBlock(BaseArtefacts.callPlan(subPlan.getId().toString()))
                .add(set("var", "'value'")) //child nodes of call plans are not executed and will not appear in the report
                .endBlock()
                .endBlock().build();

        engine.getExecutionEngineContext().getPlanAccessor().save(List.of(subPlan));

        PlanRunnerResult result = engine.execute(plan);
        result.printTree();
        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        logger.info(node.toString());
        assertEquals("For: 1x: PASSED\n" +
                        " CallPlan: 1x: PASSED\n" +
                        "  Sequence: 1x: PASSED\n" +
                        "   Echo: 1x: PASSED > in sub plan\n",
                node.toString());

    }

    @Test
    public void planWithDynamicCallPlan() throws IOException, InterruptedException {
        Plan subPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(echo("'test'"))
                .endBlock().build();
        subPlan.addAttribute(AbstractOrganizableObject.NAME, "my-sub-plan-1");

        Plan subPlan2 = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(set("key", "'value'"))
                .endBlock().build();
        subPlan2.addAttribute(AbstractOrganizableObject.NAME, "my-sub-plan-2");

        CallPlan callPlan = new CallPlan();
        callPlan.setSelectionAttributes(new DynamicValue<>("\"{\\\"name\\\":\\\"my-sub-plan-\" + gcounter + \"\\\"}\"", ""));
        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 2))
                //.add(set("planName","'my-sub-plan-' + gcounter"))
                .add(callPlan)
                .endBlock().build();

        engine.getExecutionEngineContext().getPlanAccessor().save(List.of(subPlan, subPlan2));

        PlanRunnerResult result = engine.execute(plan);
        result.printTree();
        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        logger.info(node.toString());
        assertEquals("For: 1x: PASSED\n" +
                        " CallPlan: 2x: PASSED\n" +
                        "  Sequence: 1x: PASSED\n" +
                        "   Echo: 1x: PASSED > test\n" +
                        "  Sequence: 1x: PASSED\n" +
                        "   Set: 1x: PASSED > key = value\n",
                node.toString());

    }

    @Test
    public void planWithCallPlan() throws IOException, InterruptedException {
        Plan subSubPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(echo("'Echo 4'"))
                .endBlock().build();

        Plan subPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(echo("'Echo 2'"))
                .add(echo("'Echo 3'"))
                .startBlock(BaseArtefacts.for_(1, 2))
                .add(BaseArtefacts.callPlan(subSubPlan.getId().toString()))
                .endBlock()
                .endBlock().build();

        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 10))
                .add(BaseArtefacts.callPlan(subPlan.getId().toString()))
                .add(BaseArtefacts.callPlan(subPlan.getId().toString()))
                .endBlock().build();

        engine.getExecutionEngineContext().getPlanAccessor().save(List.of(subPlan, subSubPlan));

        PlanRunnerResult result = engine.execute(plan);
        result.printTree();
        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        logger.info(node.toString());
        assertEquals("For: 1x: PASSED\n" +
                        " CallPlan: 10x: PASSED\n" +
                        "  Sequence: 10x: PASSED\n" +
                        "   Echo: 10x: PASSED\n" +
                        "   Echo: 10x: PASSED\n" +
                        "   For: 10x: PASSED\n" +
                        "    CallPlan: 20x: PASSED\n" +
                        "     Sequence: 20x: PASSED\n" +
                        "      Echo: 20x: PASSED\n" +
                        " CallPlan: 10x: PASSED\n" +
                        "  Sequence: 10x: PASSED\n" +
                        "   Echo: 10x: PASSED\n" +
                        "   Echo: 10x: PASSED\n" +
                        "   For: 10x: PASSED\n" +
                        "    CallPlan: 20x: PASSED\n" +
                        "     Sequence: 20x: PASSED\n" +
                        "      Echo: 20x: PASSED\n",
                node.toString());

    }

    @Test
    public void planWithRecursiveCallPlan() throws IOException, InterruptedException {

        ObjectId planId = new ObjectId();
        Plan subPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(BaseArtefacts.set("nextCount","input.getInt('count')+1"))
                .startBlock(BaseArtefacts.ifBlock(new DynamicValue<>("nextCount < 5", null)))
                .add(BaseArtefacts.callPlan(planId.toString(),"call sub plan","\"{\\\"count\\\":${nextCount}}\""))
                .endBlock()
                .endBlock().build();
        subPlan.setId(planId);

        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(BaseArtefacts.callPlan(subPlan.getId().toString(),"call sub plan","'{\"count\":1}'"))
                .endBlock().build();

        engine.getExecutionEngineContext().getPlanAccessor().save(List.of(subPlan));

        PlanRunnerResult result = engine.execute(plan);
        result.printTree();
        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        logger.info(node.toString());
        assertEquals("Sequence: 1x: PASSED\n" +
                        " call sub plan: 1x: PASSED\n" +
                        "  Sequence: 1x: PASSED\n" +
                        "   Set: 1x: PASSED > nextCount = 2\n" +
                        "   If: 1x: PASSED\n" +
                        "    call sub plan: 1x: PASSED\n" +
                        "     Sequence: 1x: PASSED\n" +
                        "      Set: 1x: PASSED > nextCount = 3\n" +
                        "      If: 1x: PASSED\n" +
                        "       call sub plan: 1x: PASSED\n" +
                        "        Sequence: 1x: PASSED\n" +
                        "         Set: 1x: PASSED > nextCount = 4\n" +
                        "         If: 1x: PASSED\n" +
                        "          call sub plan: 1x: PASSED\n" +
                        "           Sequence: 1x: PASSED\n" +
                        "            Set: 1x: PASSED > nextCount = 5\n" +
                        "            If: 1x: PASSED\n",
                node.toString());

        // Test partial aggregated tree for single Set
        ReportNode reportNode = engine.getExecutionEngineContext().getReportNodeAccessor().getReportNodesByExecutionIDAndClass(result.getExecutionId(), "step.artefacts.reports.SetReportNode").findFirst().orElseThrow(() -> new RuntimeException("No echo report node found"));
        AggregatedReportViewBuilder.AggregatedReportViewRequest aggregatedReportViewRequest = new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), false);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Partial aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("Sequence: 0x\n" +
                        " call sub plan: 0x\n" +
                        "  Sequence: 0x\n" +
                        "   Set: 1x: PASSED > nextCount = 2\n" +
                        "   If: 0x\n" +
                        "    call sub plan: 0x\n" +
                        "     Sequence: 0x\n" +
                        "      Set: 0x\n" +
                        "      If: 0x\n" +
                        "       call sub plan: 0x\n" +
                        "        Sequence: 0x\n" +
                        "         Set: 0x\n" +
                        "         If: 0x\n" +
                        "          call sub plan: 0x\n" +
                        "           Sequence: 0x\n" +
                        "            Set: 0x\n" +
                        "            If: 0x\n",
                node.toString());

    }

    @Test
    public void rootSequenceWithBeforeAfter() throws IOException, InterruptedException {
        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence()).withBefore(set("myVar", "'test'")).withAfter(echo("'in after'"))
                .add(echo("'myVar value is ' + myVar"))
                .endBlock().build();

        PlanRunnerResult result = engine.execute(plan);
        result.printTree();
        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        logger.info(node.toString());
        assertEquals("Sequence: 1x: PASSED\n" +
                        " [BEFORE]\n" +
                        "  Set: 1x: PASSED > myVar = test\n" +
                        " Echo: 1x: PASSED > myVar value is test\n" +
                        " [AFTER]\n" +
                        "  Echo: 1x: PASSED > in after\n",
                node.toString());
    }

    @Test
    public void subSequenceWithBeforeAfter() throws IOException, InterruptedException {
        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .startBlock(BaseArtefacts.sequence()).withBefore(set("myVar", "'test'")).withAfter(echo("'in after'"))
                .add(echo("'myVar value is ' + myVar"))
                .endBlock()
                .endBlock().build();

        PlanRunnerResult result = engine.execute(plan);
        result.printTree();
        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        logger.info(node.toString());
        assertEquals("Sequence: 1x: PASSED\n" +
                        " Sequence: 1x: PASSED\n" +
                        "  [BEFORE]\n" +
                        "   Set: 1x: PASSED > myVar = test\n" +
                        "  Echo: 1x: PASSED > myVar value is test\n" +
                        "  [AFTER]\n" +
                        "   Echo: 1x: PASSED > in after\n",
                node.toString());
    }

    @Test
    public void planWithCallPlanAndBeforeAfter() throws IOException, InterruptedException {
        Plan subSubPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(echo("'Echo ' + myVar"))
                .endBlock().build();

        Plan subPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(echo("'Echo 2'"))
                .add(echo("'Echo 3'"))
                .startBlock(BaseArtefacts.for_(1, 2))
                    .withBefore(echo("'In before'"), set("myVar", "'test'"))
                    .withAfter(echo("'in after'"))
                .add(BaseArtefacts.callPlan(subSubPlan.getId().toString()))
                .endBlock()
                .endBlock().build();

        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 10))
                .add(BaseArtefacts.callPlan(subPlan.getId().toString()))
                .add(BaseArtefacts.callPlan(subPlan.getId().toString()))
                .endBlock().build();

        engine.getExecutionEngineContext().getPlanAccessor().save(List.of(subPlan, subSubPlan));

        PlanRunnerResult result = engine.execute(plan);
        result.printTree();
        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");
        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        logger.info(node.toString());
        assertEquals("For: 1x: PASSED\n" +
                        " CallPlan: 10x: PASSED\n" +
                        "  Sequence: 10x: PASSED\n" +
                        "   Echo: 10x: PASSED\n" +
                        "   Echo: 10x: PASSED\n" +
                        "   For: 10x: PASSED\n" +
                        "    [BEFORE]\n" +
                        "     Echo: 10x: PASSED\n" +
                        "     Set: 10x: PASSED\n" +
                        "    CallPlan: 20x: PASSED\n" +
                        "     Sequence: 20x: PASSED\n" +
                        "      Echo: 20x: PASSED\n" +
                        "    [AFTER]\n" +
                        "     Echo: 10x: PASSED\n" +
                        " CallPlan: 10x: PASSED\n" +
                        "  Sequence: 10x: PASSED\n" +
                        "   Echo: 10x: PASSED\n" +
                        "   Echo: 10x: PASSED\n" +
                        "   For: 10x: PASSED\n" +
                        "    [BEFORE]\n" +
                        "     Echo: 10x: PASSED\n" +
                        "     Set: 10x: PASSED\n" +
                        "    CallPlan: 20x: PASSED\n" +
                        "     Sequence: 20x: PASSED\n" +
                        "      Echo: 20x: PASSED\n" +
                        "    [AFTER]\n" +
                        "     Echo: 10x: PASSED\n",
                node.toString());

    }

    @Test
    public void threadGroup() throws IOException, InterruptedException {
        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.threadGroup(2,5, childrenBlock(echo("userId")), childrenBlock(echo("userId")))).withBefore(set("myVar", "'test'")).withAfter(echo("'in after'"))
                .add(echo("'myVar value is ' + myVar"))
                .add(sleep(1))
                .endBlock().build();

        PlanRunnerResult result = engine.execute(plan);
        result.printTree();
        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        logger.info(node.toString());

        logger.info(node.toString());
        assertEquals("ThreadGroup: 1x: PASSED\n" +
                        " [BEFORE]\n" +
                        "  Set: 1x: PASSED > myVar = test\n" +
                        " [BEFORE_THREAD]\n" +
                        "  Echo: 2x: PASSED\n" +
                        " Echo: 10x: PASSED\n" +
                        " Sleep: 10x: PASSED\n" +
                        " [AFTER_THREAD]\n" +
                        "  Echo: 2x: PASSED\n" +
                        " [AFTER]\n" +
                        "  Echo: 1x: PASSED > in after\n",
                node.toString());

        // Test partial aggregated tree, starting from the Set node in "Before"
        ReportNode reportNode = engine.getExecutionEngineContext().getReportNodeAccessor().getReportNodesByExecutionIDAndClass(result.getExecutionId(), "step.artefacts.reports.SetReportNode").findFirst().orElseThrow(() -> new RuntimeException("No set report node found"));
        AggregatedReportViewBuilder.AggregatedReportViewRequest aggregatedReportViewRequest = new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), false);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Partial aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("ThreadGroup: 0x\n" +
                        " [BEFORE]\n" +
                        "  Set: 1x: PASSED > myVar = test\n" +
                        " [BEFORE_THREAD]\n" +
                        "  Echo: 0x\n" +
                        " Echo: 0x\n" +
                        " Sleep: 0x\n" +
                        " [AFTER_THREAD]\n" +
                        "  Echo: 0x\n" +
                        " [AFTER]\n" +
                        "  Echo: 0x\n",
                node.toString());

        // Test partial aggregated tree, starting from the Set node in "Before" and filtering the report
        aggregatedReportViewRequest = new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), true);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Partial aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("ThreadGroup: 0x\n" +
                        " [BEFORE]\n" +
                        "  Set: 1x: PASSED > myVar = test\n",
                node.toString());


        // Test partial aggregated tree, starting from the Sleep node in the thread
        reportNode = engine.getExecutionEngineContext().getReportNodeAccessor().getReportNodesByExecutionIDAndClass(result.getExecutionId(), "step.artefacts.reports.SleepReportNode").findFirst().orElseThrow(() -> new RuntimeException("No set report node found"));
        aggregatedReportViewRequest = new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), false);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Partial aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("ThreadGroup: 0x\n" +
                        " [BEFORE]\n" +
                        "  Set: 0x\n" +
                        " [BEFORE_THREAD]\n" +
                        "  Echo: 0x\n" +
                        " Echo: 1x: PASSED > myVar value is test\n" +
                        " Sleep: 1x: PASSED\n" +
                        " [AFTER_THREAD]\n" +
                        "  Echo: 0x\n" +
                        " [AFTER]\n" +
                        "  Echo: 0x\n",
                node.toString());

        // Test partial aggregated tree, starting from the Set node in "Before" and filtering the report
        aggregatedReportViewRequest = new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), true);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Partial aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("ThreadGroup: 0x\n" +
                        " Echo: 1x: PASSED > myVar value is test\n" +
                        " Sleep: 1x: PASSED\n",
                node.toString());

    }

    @Test
    public void testScenario() throws IOException, InterruptedException {
        Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.testScenario())
                .startBlock(BaseArtefacts.threadGroup(2,5))
                .add(echo("'in 1st thread gorup'"))
                .add(sleep(1))
                .endBlock()
                .startBlock(BaseArtefacts.threadGroup(2,5))
                .add(echo("'in 2nd thread group'"))
                .add(set("key","'value'"))
                .endBlock()
                .endBlock().build();


        PlanRunnerResult result = engine.execute(plan);
        result.printTree();
        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        logger.info(node.toString());

        logger.info(node.toString());
        assertEquals("TestScenario: 1x: PASSED\n" +
                        " ThreadGroup: 1x: PASSED\n" +
                        "  Echo: 10x: PASSED\n" +
                        "  Sleep: 10x: PASSED\n" +
                        " ThreadGroup: 1x: PASSED\n" +
                        "  Echo: 10x: PASSED\n" +
                        "  Set: 10x: PASSED\n",
                node.toString());

        // Test partial aggregated tree, starting from the Set node
        ReportNode reportNode = engine.getExecutionEngineContext().getReportNodeAccessor().getReportNodesByExecutionIDAndClass(result.getExecutionId(), "step.artefacts.reports.SetReportNode").findFirst().orElseThrow(() -> new RuntimeException("No set report node found"));
        AggregatedReportViewBuilder.AggregatedReportViewRequest aggregatedReportViewRequest = new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), false);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Partial aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("TestScenario: 0x\n" +
                        " ThreadGroup: 0x\n" +
                        "  Echo: 0x\n" +
                        "  Sleep: 0x\n" +
                        " ThreadGroup: 0x\n" +
                        "  Echo: 1x: PASSED > in 2nd thread group\n" +
                        "  Set: 1x: PASSED > key = value\n",
                node.toString());

        // Test partial aggregated tree, starting from the Set node and filtering the report
        aggregatedReportViewRequest = new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, true, reportNode.getId().toHexString(), true);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Partial aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("TestScenario: 0x\n" +
                        " ThreadGroup: 0x\n" +
                        "  Echo: 1x: PASSED > in 2nd thread group\n" +
                        "  Set: 1x: PASSED > key = value\n",
                node.toString());

    }

}