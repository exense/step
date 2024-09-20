package step.reporting;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.artefacts.reports.aggregated.AggregatedReportViewBuilder;
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
import static step.planbuilder.BaseArtefacts.*;

public class ResolvedPlanBuilderTest {

    protected static final Logger logger = LoggerFactory.getLogger(ResolvedPlanBuilderTest.class);

    private ExecutionEngine engine;

    @Before
    public void before() {
        engine = new ExecutionEngine.Builder().withPlugin(new BaseArtefactPlugin()).withPlugin(new ThreadPoolPlugin())
                .withPlugin(new FunctionPlugin()).withPlugin(new TokenForecastingExecutionPlugin()).build();
    }

    @After
    public void after() {
        engine.close();
    }

    //@Test
    public void get() throws IOException, InterruptedException {
        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 10))
                .add(echo("'test'"))
                .startBlock(BaseArtefacts.for_(1, 5))
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
        assertEquals("ForBlock: 1x\n" +
                " Echo: 10x\n" +
                " ForBlock: 10x\n" +
                "  Echo: 50x\n" +
                "  Echo: 50x\n",
                node.toString());
    }

    //@Test
    // Uncommenting until merge of SED-3350
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
        assertEquals("ForBlock: 1x\n" +
                        " CallPlan: 10x\n" +
                        "  Sequence: 10x\n" +
                        "   Echo: 10x\n" +
                        "   Echo: 10x\n" +
                        "   ForBlock: 10x\n" +
                        "    CallPlan: 20x\n" +
                        "     Sequence: 20x\n" +
                        "      Echo: 20x\n" +
                        " CallPlan: 10x\n" +
                        "  Sequence: 10x\n" +
                        "   Echo: 10x\n" +
                        "   Echo: 10x\n" +
                        "   ForBlock: 10x\n" +
                        "    CallPlan: 20x\n" +
                        "     Sequence: 20x\n" +
                        "      Echo: 20x\n",
                node.toString());

    }


}