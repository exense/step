package step.plugins.functions.types;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.Return;
import step.artefacts.handlers.functions.TokenForcastingExecutionPlugin;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.artefacts.reports.aggregated.AggregatedReportViewBuilder;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.Function;
import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.threadpool.ThreadPoolPlugin;

import javax.json.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CompositeResolvedPlanBuilderTest {

    private ExecutionEngine engine;

    @Before
    public void before() {
        engine = new ExecutionEngine.Builder().withPlugin(new BaseArtefactPlugin()).withPlugin(new ThreadPoolPlugin())
                .withPlugin(new FunctionPlugin()).withPlugin(new AbstractExecutionEnginePlugin() {
                    @Override
                    public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
                        super.initializeExecutionContext(executionEngineContext, executionContext);
                        FunctionTypeRegistry functionTypeRegistry = executionContext.require(FunctionTypeRegistry.class);
                        functionTypeRegistry.registerFunctionType(new CompositeFunctionType(null));
                        functionTypeRegistry.registerFunctionType(new CustomFunctionType());
                    }
                }).withPlugin(new TokenForcastingExecutionPlugin()).build();
    }

    @After
    public void after() {
        engine.close();
    }


    @Test
    public void planWithCallComposite() throws IOException, InterruptedException {
        Return aReturn = new Return();
        aReturn.setOutput(new DynamicValue<>("{\"myOut\":\"test\"}"));

        Plan compositePlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(aReturn)
                .endBlock().build();

        CompositeFunction compositeFunction = new CompositeFunction();
        compositeFunction.addAttribute(AbstractOrganizableObject.NAME, "MyComposite");
        compositeFunction.setPlan(compositePlan);

        CallFunction myComposite = FunctionArtefacts.keyword("MyComposite");
        myComposite.addChild(BaseArtefacts.check("output.myOut == 'test'"));

        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                .add(myComposite)
                .endBlock().build();

        plan.setFunctions(List.of(compositeFunction));

        PlanRunnerResult result = engine.execute(plan);
        result.printTree();
        System.out.println("----------------------");
        System.out.println("Aggregated report tree");
        System.out.println("----------------------");

        // Sleep a few ms to ensure that the report node timeseries is flushed
        Thread.sleep(100);

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        System.out.println(node.toString());
        Assert.assertEquals("Sequence: 1x\n" +
                " CallFunction: 1x\n" +
                "  Sequence: 1x\n" +
                "   Return: 1x\n" +
                "  Check: 1x\n",
                node.toString());
    }

    @Test
    public void planWithCallFunction() throws IOException, InterruptedException {
        CustomFunction myFunction = new CustomFunction();
        myFunction.addAttribute(AbstractOrganizableObject.NAME, "My function call");

        Plan plan = PlanBuilder.create()
                .startBlock(FunctionArtefacts.keyword("My function call")).withBefore(BaseArtefacts.echo("'test'")).add(BaseArtefacts.check("true"))
                .endBlock().build();


        plan.setFunctions(List.of(myFunction));

        PlanRunnerResult result = engine.execute(plan);
        result.printTree();
        System.out.println("----------------------");
        System.out.println("Aggregated report tree");
        System.out.println("----------------------");

        // Sleep a few ms to ensure that the report node timeseries is flushed
        Thread.sleep(100);

        AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(engine.getExecutionEngineContext(), result.getExecutionId());
        AggregatedReportView node = aggregatedReportViewBuilder.buildAggregatedReportView();
        System.out.println(node.toString());
        Assert.assertEquals("CallFunction: 1x\n" +
                " [BEFORE]\n" +
                "  Echo: 1x\n" +
                " Check: 1x\n",
                node.toString());
    }

    public static class CustomFunction extends Function {

    }

    public static class CustomFunctionType extends AbstractFunctionType<CustomFunction> {

        @Override
        public String getHandlerChain(CustomFunction function) {
            return MyFunctionHandler.class.getName();
        }

        @Override
        public Map<String, String> getHandlerProperties(CustomFunction function, AbstractStepContext executionContext) {
            return null;
        }

        @Override
        public CustomFunction newFunction() {
            return new CustomFunction();
        }

    }

    public static class MyFunctionHandler extends JsonBasedFunctionHandler {
        @Override
        public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
            return new OutputBuilder().build();
        }
    }
}