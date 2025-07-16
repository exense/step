package step.artefacts.handlers;

import static org.junit.Assert.*;

import org.junit.Test;

import step.artefacts.CallPlan;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectFilter;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.expressions.ExpressionHandler;

import java.util.NoSuchElementException;

public class PlanLocatorTest {

	public static final String CUSTOM_ATTR_1 = "customAttr1";
	public static final String CUSTOM_ATTR_1_VALUE = "customAttr1Value";
	public static final String CUSTOM_ATTR_2_VALUE = "customAttr2Value";
	private final PlanAccessor planAccessor = new InMemoryPlanAccessor();
	private final PlanLocator planLocator = new PlanLocator(planAccessor, new SelectorHelper(
			new DynamicJsonObjectResolver(new DynamicJsonValueResolver(new ExpressionHandler()))));

	@Test
	public void test() {
		Plan expectedPlan1 = newPlan("name1", CUSTOM_ATTR_1_VALUE);
		Plan expectedPlan2 = newPlan("name2", CUSTOM_ATTR_2_VALUE);
		
		// By id
		CallPlan artefact = new CallPlan();
		artefact.setPlanId(expectedPlan1.getId().toString());
		Plan actualPlan1 = planLocator.selectPlan(artefact, objectFilter(), null);
		assertEquals(expectedPlan1, actualPlan1);
		
		// By attributes
		CallPlan artefact2 = new CallPlan();
		artefact2.setSelectionAttributes(new DynamicValue<String>("{\"name\":\"name1\"}"));
		Plan actualPlan2 = planLocator.selectPlan(artefact2, objectFilter(), null);
		assertEquals(expectedPlan1, actualPlan2);
		
		// With predicate
		CallPlan artefact3 = new CallPlan();
		assertThrows("No selection attribute defined", NoSuchElementException.class, () -> planLocator.selectPlan(artefact3, objectFilter(CUSTOM_ATTR_1, CUSTOM_ATTR_1_VALUE), null));

		artefact3.setSelectionAttributes(new DynamicValue<String>("{\"name\":\"name1\"}"));
		Plan actualPlan3 = planLocator.selectPlan(artefact3, objectFilter(CUSTOM_ATTR_1, CUSTOM_ATTR_1_VALUE), null);
		assertEquals(expectedPlan1, actualPlan3);
		artefact3.setSelectionAttributes(new DynamicValue<String>("{\"name\":\"name2\"}"));
		actualPlan3 = planLocator.selectPlan(artefact3, objectFilter(CUSTOM_ATTR_1, CUSTOM_ATTR_2_VALUE), null);
		assertEquals(expectedPlan2, actualPlan3);
	}

	private Plan newPlan(String name, String customAttrValue) {
		Plan plan = new Plan();
		plan.addAttribute(AbstractOrganizableObject.NAME, name);
		plan.addAttribute(CUSTOM_ATTR_1, customAttrValue);
		planAccessor.save(plan);
		return plan;
	}

	private ObjectFilter objectFilter() {
		return () -> "";
	}
	
	private ObjectFilter objectFilter(String key, String value) {
		return () -> "attributes." + key + " = " + value;
	}

}
