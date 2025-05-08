package step.plugins.threadmanager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.common.managedoperations.Operation;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.artefacts.reports.aggregated.AggregatedReportViewBuilder;
import step.core.artefacts.reports.aggregated.AggregatedReportViewRequest;
import step.core.execution.ExecutionEngine;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.threadmanager.ThreadManager;
import step.engine.plugins.FunctionPlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.plans.assertions.PerformanceAssertPlugin;
import step.threadpool.ThreadPoolPlugin;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static step.planbuilder.BaseArtefacts.echo;
import static step.planbuilder.BaseArtefacts.sleep;

public class ThreadManagerPluginTest {

    protected static final Logger logger = LoggerFactory.getLogger(ThreadManagerPluginTest.class);

    private ExecutionEngine engine;

    @Before
    public void before() {
        ThreadManager threadManager = new ThreadManager();
        engine = new ExecutionEngine.Builder()
                .withPlugin(new BaseArtefactPlugin())
                .withPlugin(new ThreadPoolPlugin())
                .withPlugin(new FunctionPlugin())
                .withPlugin(new LocalFunctionPlugin())
                .withPlugin(new PerformanceAssertPlugin())
                .withPlugin(new TokenForecastingExecutionPlugin())
                .withPlugin(new ThreadManagerPlugin(threadManager)).build();
    }

    @After
    public void after() {
        engine.close();
    }

    @Test
    public void testAggregatedReportWithRunningNodesAndCurrentOperations() throws IOException, ExecutionException, InterruptedException {
        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 4, 2))
                .startBlock(FunctionArtefacts.session())
                .add(echo("'Echo'"))
                .add(sleep(1000))
                .endBlock()
                .endBlock().build();
        ExecutionParameters executionParameters = new ExecutionParameters();
        executionParameters.setPlan(plan);
        String executionId = engine.initializeExecution(executionParameters);

        //Start exec async
        CompletableFuture<PlanRunnerResult> planRunnerResultCompletableFuture = CompletableFuture.supplyAsync(() -> {
            // Your async code here
            return engine.execute(executionId);
        });

        //Poll until running (can take some time)
        ExecutionAccessor executionAccessor = engine.getExecutionEngineContext().getExecutionAccessor();
        pollUntilTrue(() -> executionAccessor.get(executionId).getStatus().equals(ExecutionStatus.RUNNING),
                100, 10000);

        //give some time for the execution to start
        Thread.sleep(500);
        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), executionId);
        AggregatedReportViewRequest aggregatedReportViewRequest = new AggregatedReportViewRequest(null, true, null, false, null,  true);
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        logger.info("----------------------");
        logger.info("Aggregated report tree");
        logger.info("----------------------");
        logger.info(node.toString());

        assertEquals("For: 1x: RUNNING\n" +
                        " Session: 2x: RUNNING\n" +
                        "  Echo: 2x: PASSED\n" +
                        "  Sleep: 2x: RUNNING\n",
                node.toString());

        List<Operation> currentOperationsFor = node.currentOperations;
        assertEquals(2, currentOperationsFor.size());
        String regexExpected = "\\[Operation\\{name='Sleep', start=[^,]+, details=\\{Sleep time=00:00:01.000, Release token=false\\}, reportNodeId='[^']+', artefactHash='[^']+', tid=[0-9]+\\}, Operation\\{name='Sleep', start=[^,]*, details=\\{Sleep time=00:00:01.000, Release token=false\\}, reportNodeId='[^']+', artefactHash='[^']+', tid=[0-9]+\\}\\]";
        assertTrue(currentOperationsFor.toString().matches(regexExpected));
        List<Operation> currentOperationsSleep = node.children.get(0).children.get(1).currentOperations;
        assertTrue(currentOperationsSleep.toString().matches(regexExpected));

        //patral tree with running nodes and operations
        ReportNode reportNode = engine.getExecutionEngineContext().getReportNodeAccessor().getReportNodesByExecutionIDAndClass(executionId, "step.artefacts.reports.EchoReportNode").findFirst().orElseThrow(() -> new RuntimeException("No echo report node found"));
        AggregatedReportViewRequest partialAggregatedReportViewRequest =
                new AggregatedReportViewRequest(null, true, reportNode.getId().toString(), false, null, true);
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);

        PlanRunnerResult result = planRunnerResultCompletableFuture.get();
        aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        node = aggregatedReportViewBuilder.buildAggregatedReportView(aggregatedReportViewRequest);
        assertEquals("For: 1x: PASSED\n" +
                        " Session: 4x: PASSED\n" +
                        "  Echo: 4x: PASSED\n" +
                        "  Sleep: 4x: PASSED\n",
                node.toString());

        currentOperationsFor = node.currentOperations;
        assertEquals(0, currentOperationsFor.size());

    }

    public void pollUntilTrue(Supplier<Boolean> condition, long pollIntervalMillis, long timeoutMillis) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (!condition.get()) {
            if (System.currentTimeMillis() - start > timeoutMillis) {
                throw new RuntimeException("Condition not met within timeout");
            }
            Thread.sleep(pollIntervalMillis);
        }
    }

}