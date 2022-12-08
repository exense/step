package step.artefacts.handlers;

import static org.junit.Assert.*;

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
		
		// With predicate
		CallPlan artefact3 = new CallPlan();
		artefact2.setDynamicName(new DynamicValue<String>("{}"));
		Plan actualPlan3 = planLocator.selectPlan(artefact3, objectPredicate("name1"), null);
		assertEquals(expectedPlan1, actualPlan3);
		actualPlan3 = planLocator.selectPlan(artefact3, objectPredicate("name2"), null);
		assertEquals(expectedPlan2, actualPlan3);
	}

	private Plan newPlan(String name) {
		Plan plan = new Plan();
		plan.setName(name);
		planAccessor.save(plan);
		return plan;
	}

	private ObjectPredicate objectPredicate() {
		return t -> true;
	}
	
	private ObjectPredicate objectPredicate(String name) {
		return t -> ((Plan)t).getname().equals(name);
	}

}
