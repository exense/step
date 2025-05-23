import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.*;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.artefacts.reports.aggregated.AggregatedReportViewBuilder;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.OperationMode;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.type.FunctionTypeRegistry;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.plans.assertions.PerformanceAssertPlugin;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.CompositeFunctionType;
import step.threadpool.ThreadPoolPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ResolvedPlanBuilderForCompositeKeywordTest {

    public static final String MY_COMPOSITE = "MyComposite";

    private ExecutionEngine engine;

    @Before
    public void before() {
        ExecutionEngineContext parentContext = new ExecutionEngineContext(OperationMode.LOCAL, true);
        InMemoryFunctionAccessorImpl functionAccessor = new InMemoryFunctionAccessorImpl();
        parentContext.put(FunctionAccessor.class, functionAccessor);
        engine = new ExecutionEngine.Builder().withParentContext(parentContext)
                .withPlugin(new FunctionPlugin())
                .withPlugin(new BaseArtefactPlugin())
                .withPlugin(new TokenForecastingExecutionPlugin())
                .withPlugin(new ThreadPoolPlugin())
                .withPlugin(new PerformanceAssertPlugin())
                .withPlugin(new AbstractExecutionEnginePlugin() {
                    @Override
                    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
                        super.initializeExecutionContext(executionEngineContext, executionContext);
                        FunctionTypeRegistry functionTypeRegistry = executionContext.require(FunctionTypeRegistry.class);
                        functionTypeRegistry.registerFunctionType(new CompositeFunctionType(null));
                    }
                }).build();
    }

    @After
    public void after() {
        engine.close();
    }

    @Test
    public void planWithCallKeyword() throws IOException, InterruptedException {

        Return aReturn = new Return();
        aReturn.setOutput(new DynamicValue<>("{\"myOutput\":\"some output values\"}"));
        Plan compositePlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                    .add(BaseArtefacts.echo("'Echo 4'"))
                    .add(aReturn)
                .endBlock().build();

        CompositeFunction compositeFunction = new CompositeFunction();
        compositeFunction.addAttribute(AbstractOrganizableObject.NAME, MY_COMPOSITE);
        compositeFunction.setPlan(compositePlan);

        PerformanceAssert performanceAssert = new PerformanceAssert();
        Filter filterByName = new Filter(AbstractOrganizableObject.NAME, MY_COMPOSITE, FilterType.EQUALS);
        performanceAssert.setFilters(new ArrayList<>(List.of(filterByName)));

        Plan subPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                    .startBlock(FunctionArtefacts.keyword(MY_COMPOSITE)).withAfter(performanceAssert)
                        .add(BaseArtefacts.check("output.myOutput == 'some output values'"))
                    .endBlock()
                .endBlock().build();

        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 10))
                    .add(BaseArtefacts.callPlan(subPlan.getId().toString()))
                .endBlock().build();

        ExecutionEngineContext executionEngineContext = engine.getExecutionEngineContext();
        executionEngineContext.getPlanAccessor().save(List.of(subPlan));
        executionEngineContext.get(FunctionAccessor.class).save(compositeFunction);

        PlanRunnerResult result = engine.execute(plan);

        // Sleep a few ms to ensure that the report node timeseries is flushed
        Thread.sleep(500);

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        result.printTree();
        System.out.println("----------------------");
        System.out.println("Aggregated report tree");
        System.out.println("----------------------");
        System.out.println(node.toString());

        Assert.assertEquals("For: 1x: PASSED\n" +
                        " CallPlan: 10x: PASSED\n" +
                        "  Sequence: 10x: PASSED\n" +
                        "   MyComposite: 10x: PASSED\n" +
                        "    Sequence: 10x: PASSED\n" +
                        "     Echo: 10x: PASSED\n" +
                        "     Return: 10x: PASSED\n" +
                        "    [AFTER]\n" +
                        "     PerformanceAssert: 10x: PASSED\n" +
                        "    Check: 10x: PASSED\n",
                node.toString());
    }

    @Test
    public void planWithCallKeywordDynamic() throws IOException, InterruptedException {

        Return aReturn = new Return();
        aReturn.setOutput(new DynamicValue<>("{\"myOutput\":\"some output values\"}"));
        Plan compositePlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(BaseArtefacts.echo("'Echo 4'"))
                .add(aReturn)
                .endBlock().build();

        CompositeFunction compositeFunction = new CompositeFunction();
        compositeFunction.addAttribute(AbstractOrganizableObject.NAME, MY_COMPOSITE + "1");
        compositeFunction.setPlan(compositePlan);

        CompositeFunction compositeFunction2 = new CompositeFunction();
        compositeFunction2.addAttribute(AbstractOrganizableObject.NAME, MY_COMPOSITE  + "2");
        compositeFunction2.setPlan(compositePlan);

        CallFunction callFunction = new CallFunction();
        callFunction.setFunction(new DynamicValue<>("\"{\\\"name\\\":\\\"" + MY_COMPOSITE + "\" + gcounter + \"\\\"}\"", ""));


        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 2))
                .startBlock(callFunction)
                .add(BaseArtefacts.check("output.myOutput == 'some output values'"))
                .endBlock()
                .endBlock().build();

        ExecutionEngineContext executionEngineContext = engine.getExecutionEngineContext();
        executionEngineContext.get(FunctionAccessor.class).save(compositeFunction);
        executionEngineContext.get(FunctionAccessor.class).save(compositeFunction2);

        PlanRunnerResult result = engine.execute(plan);

        // Sleep a few ms to ensure that the report node timeseries is flushed
        Thread.sleep(500);

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        result.printTree();
        System.out.println("----------------------");
        System.out.println("Aggregated report tree");
        System.out.println("----------------------");
        System.out.println(node.toString());

        Assert.assertEquals("For: 1x: PASSED\n" +
                        " CallKeyword: 2x: PASSED\n" +
                        "  Sequence: 2x: PASSED\n" +
                        "   Echo: 2x: PASSED\n" +
                        "   Return: 2x: PASSED\n" +
                        "  Check: 2x: PASSED\n",

                node.toString());
    }
}