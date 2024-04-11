import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.handlers.functions.TokenAutoscalingExecutionPlugin;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.aggregatedtree.AggregatedReportTreeNavigator;
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
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.CompositeFunctionType;
import step.threadpool.ThreadPoolPlugin;

import java.io.IOException;
import java.util.List;

public class AggregatedReportTreeNavigatorForCompositeKeywordTest {

    public static final String MY_COMPOSITE = "MyComposite";

    private ExecutionEngine engine;

    @Before
    public void before() {
        ExecutionEngineContext parentContext = new ExecutionEngineContext(OperationMode.LOCAL);
        InMemoryFunctionAccessorImpl functionAccessor = new InMemoryFunctionAccessorImpl();
        parentContext.put(FunctionAccessor.class, functionAccessor);
        engine = new ExecutionEngine.Builder().withParentContext(parentContext).withPlugin(new FunctionPlugin())
                .withPlugin(new BaseArtefactPlugin()).withPlugin(new TokenAutoscalingExecutionPlugin()).withPlugin(new ThreadPoolPlugin()).withPlugin(new AbstractExecutionEnginePlugin() {
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
    public void planWithCallKeyword() throws IOException {

        Plan compositePlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                    .add(BaseArtefacts.echo("'Echo 4'"))
                .endBlock().build();

        CompositeFunction compositeFunction = new CompositeFunction();
        compositeFunction.addAttribute(AbstractOrganizableObject.NAME, MY_COMPOSITE);
        compositeFunction.setPlan(compositePlan);

        Plan subPlan = PlanBuilder.create()
                .startBlock(BaseArtefacts.sequence())
                    .add(FunctionArtefacts.keyword(MY_COMPOSITE))
                .endBlock().build();

        Plan plan = PlanBuilder.create()
                .startBlock(BaseArtefacts.for_(1, 10))
                    .add(BaseArtefacts.callPlan(subPlan.getId().toString()))
                .endBlock().build();

        ExecutionEngineContext executionEngineContext = engine.getExecutionEngineContext();
        executionEngineContext.getPlanAccessor().save(List.of(subPlan));
        executionEngineContext.get(FunctionAccessor.class).save(compositeFunction);

        PlanRunnerResult result = engine.execute(plan);
        AggregatedReportTreeNavigator reportTree = new AggregatedReportTreeNavigator(executionEngineContext);
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