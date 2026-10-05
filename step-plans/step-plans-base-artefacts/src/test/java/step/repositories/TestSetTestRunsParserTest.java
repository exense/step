package step.repositories;

import org.junit.Test;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.repositories.TestRunStatus;
import step.expressions.ExpressionHandler;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static step.planbuilder.BaseArtefacts.*;

public class TestSetTestRunsParserTest {

    @Test
    public void testDirectChildrenTestCasesOnly() {
        InMemoryPlanAccessor planAccessor = new InMemoryPlanAccessor();
        Plan calledTestCasePlan = PlanBuilder.create().startBlock(testCase("TC3")).endBlock().build();
        Plan calledSequencePlan = PlanBuilder.create().startBlock(sequence()).endBlock().build();
        planAccessor.save(calledTestCasePlan);
        planAccessor.save(calledSequencePlan);

        Plan plan = PlanBuilder.create().startBlock(testSet("TestSet"))
            .startBlock(testCase("TC1")).endBlock()
            .startBlock(testCase("TC2")).endBlock()
            .add(callPlan(calledTestCasePlan.getId().toString()))
            .add(callPlan(calledSequencePlan.getId().toString()))
            .startBlock(sequence()).startBlock(testCase("Nested")).endBlock().endBlock()
            .endBlock().build();

        try (ExpressionHandler expressionHandler = new ExpressionHandler()) {
            TestSetTestRunsParser parser = new TestSetTestRunsParser(planAccessor, expressionHandler);

            List<TestRunStatus> testRuns = parser.getTestRuns(plan, o -> true);
            assertEquals(List.of("TC1", "TC2", "TC3"), testRuns.stream().map(TestRunStatus::getTestplanName).collect(Collectors.toList()));
            assertEquals(calledTestCasePlan.getRoot().getId().toString(), testRuns.get(2).getId());

            assertTrue(parser.getTestRuns(calledTestCasePlan, o -> true).isEmpty());
        }
    }
}
