package step.artefacts.handlers;

import static org.junit.Assert.*;

import java.util.NoSuchElementException;

import org.junit.Test;

import step.artefacts.CallPlan;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.expressions.ExpressionHandler;

public class PlanLocatorTest {

    private static final String CATEGORY = "category";

    private final PlanAccessor planAccessor = new InMemoryPlanAccessor();
    private final PlanLocator planLocator = new PlanLocator(planAccessor, new SelectorHelper(
        new DynamicJsonObjectResolver(new DynamicJsonValueResolver(new ExpressionHandler()))));

    @Test
    public void test() {
        Plan expectedPlan1 = newPlan("name1");
        Plan expectedPlan2 = newPlan("name2");

        // By id
        CallPlan artefact = new CallPlan();
        artefact.setPlanId(expectedPlan1.getId().toString());
        Plan actualPlan1 = planLocator.selectPlan(artefact, objectPredicate(), null);
        assertEquals(expectedPlan1, actualPlan1);

        // By attributes
        CallPlan artefact2 = new CallPlan();
        artefact2.setSelectionAttributes(new DynamicValue<String>("{\"name\":\"name1\"}"));
        Plan actualPlan2 = planLocator.selectPlan(artefact2, objectPredicate(), null);
        assertEquals(expectedPlan1, actualPlan2);

        // With predicate, selecting by an attribute shared by both plans
        CallPlan artefact3 = new CallPlan();
        artefact3.setSelectionAttributes(new DynamicValue<String>("{\"" + CATEGORY + "\":\"common\"}"));
        Plan actualPlan3 = planLocator.selectPlan(artefact3, objectPredicate("name1"), null);
        assertEquals(expectedPlan1, actualPlan3);
        actualPlan3 = planLocator.selectPlan(artefact3, objectPredicate("name2"), null);
        assertEquals(expectedPlan2, actualPlan3);
    }

    @Test
    public void testWithoutSelectionCriteria() {
        newPlan("name1");
        newPlan("name2");

        assertNoPlanSelected(new CallPlan());

        CallPlan nullSelectionAttributes = new CallPlan();
        nullSelectionAttributes.setSelectionAttributes(new DynamicValue<>(null));
        assertNoPlanSelected(nullSelectionAttributes);

        CallPlan blankSelectionAttributes = new CallPlan();
        blankSelectionAttributes.setSelectionAttributes(new DynamicValue<>(" "));
        assertNoPlanSelected(blankSelectionAttributes);
    }

    private void assertNoPlanSelected(CallPlan callPlan) {
        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> planLocator.selectPlan(callPlan, objectPredicate(), null));
        assertEquals("No selection attribute defined", exception.getMessage());
        assertThrows(PlanLocator.PlanLocatorException.class, () -> planLocator.selectPlanNotNull(callPlan, objectPredicate(), null));
        assertTrue(planLocator.getMatchingPlans(callPlan, objectPredicate(), null).isEmpty());
    }

    private Plan newPlan(String name) {
        Plan plan = new Plan();
        plan.addAttribute(AbstractOrganizableObject.NAME, name);
        plan.addAttribute(CATEGORY, "common");
        planAccessor.save(plan);
        return plan;
    }

    private ObjectPredicate objectPredicate() {
        return t -> true;
    }

    private ObjectPredicate objectPredicate(String name) {
        return t -> ((Plan) t).getAttribute(AbstractOrganizableObject.NAME).equals(name);
    }

}
