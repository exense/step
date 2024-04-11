package step.reporting;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.handlers.functions.TokenAutoscalingExecutionPlugin;
import step.core.artefacts.reports.aggregatedtree.AggregatedReportTreeNavigator;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.FunctionPlugin;
import step.planbuilder.BaseArtefacts;
import step.threadpool.ThreadPoolPlugin;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AggregatedReportTreeNavigatorTest {

    private ExecutionEngine engine;

    @Before
    public void before() {
        engine = new ExecutionEngine.Builder().withPlugin(new BaseArtefactPlugin()).withPlugin(new ThreadPoolPlugin())
                .withPlugin(new FunctionPlugin()).withPlugin(new TokenAutoscalingExecutionPlugin()).build();
    }

    @After
    public void after() {
        engine.close();
    }

    @Test
    public void get() throws IOException {
        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 10))
                .add(BaseArtefacts.echo("'test'"))
                .startBlock(BaseArtefacts.for_(1, 5))
                .add(BaseArtefacts.echo("'Echo 2'"))
                .add(BaseArtefacts.echo("'Echo 3'"))
                .endBlock()
                .endBlock().build();
        PlanRunnerResult result = engine.execute(plan);
        AggregatedReportTreeNavigator reportTree = new AggregatedReportTreeNavigator(engine.getExecutionEngineContext());
        AggregatedReportTreeNavigator.Node node = reportTree.getAggregatedReportTree(result.getExecutionId());
        result.printTree();

        assertEquals(10, node.children.get(0).callCount);
        assertEquals(10, node.children.get(1).callCount);
        assertEquals(50, node.children.get(1).children.get(0).callCount);
        assertEquals(50, node.children.get(1).children.get(1).callCount);
    }

    @Test
    public void planWithCallPlan() throws IOException {
        Plan subSubPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(BaseArtefacts.echo("'Echo 4'"))
                .endBlock().build();

        Plan subPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(BaseArtefacts.echo("'Echo 2'"))
                .add(BaseArtefacts.echo("'Echo 3'"))
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
        AggregatedReportTreeNavigator reportTree = new AggregatedReportTreeNavigator(engine.getExecutionEngineContext());
        AggregatedReportTreeNavigator.Node node = reportTree.getAggregatedReportTree(result.getExecutionId());
        result.printTree();
        System.out.println("----------------------");
        System.out.println("Aggregated report tree");
        System.out.println("----------------------");
        System.out.println(node.toString());

        System.out.println("----------------------");
        System.out.println("Report nodes for 1st CallPlan");
        System.out.println("----------------------");
        String artefactHash = node.children.get(0).artefactHash;
        reportTree.getNodesByArtefactHash(artefactHash).forEach(System.out::println);
    }

    @Test
    public void planWithCallKeyword() throws IOException {
        Plan subSubPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(BaseArtefacts.echo("'Echo 4'"))
                .endBlock().build();

        Plan subPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(BaseArtefacts.echo("'Echo 2'"))
                .add(BaseArtefacts.echo("'Echo 3'"))
                .startBlock(BaseArtefacts.for_(1, 2))
                .add(BaseArtefacts.callPlan(subSubPlan.getId().toString()))
                .endBlock()
                .endBlock().build();

        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 10))
                .add(BaseArtefacts.callPlan(subPlan.getId().toString()))
                .add(BaseArtefacts.callPlan(subPlan.getId().toString()))
                .endBlock().build();
        plan.setSubPlans(List.of(subPlan, subSubPlan));

        PlanRunnerResult result = engine.execute(plan);
        AggregatedReportTreeNavigator reportTree = new AggregatedReportTreeNavigator(engine.getExecutionEngineContext());
        AggregatedReportTreeNavigator.Node node = reportTree.getAggregatedReportTree(result.getExecutionId());
        result.printTree();
        System.out.println("----------------------");
        System.out.println("Aggregated report tree");
        System.out.println("----------------------");
        System.out.println(node.toString());

        System.out.println("----------------------");
        System.out.println("Report nodes for 1st CallPlan");
        System.out.println("----------------------");
        String artefactHash = node.children.get(0).artefactHash;
        reportTree.getNodesByArtefactHash(artefactHash).forEach(System.out::println);
    }
}