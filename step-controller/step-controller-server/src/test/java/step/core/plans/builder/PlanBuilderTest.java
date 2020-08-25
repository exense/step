package step.core.plans.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.plans.Plan;

public class PlanBuilderTest {

	@Test
	public void testNoRoot() {
		Exception ex = null;
		try {
			PlanBuilder.create().add(artefact("Root"));
		} catch(Exception e) {
			ex = e;
		}
		assertNotNull(ex);
		assertEquals("No root artefact defined. Please first call the method startBlock to define the root element", ex.getMessage());
	}
	
	@Test
	public void testUnablancedBlock() {
		Exception ex = null;
		try {
			PlanBuilder.create().startBlock(artefact("Root")).build();
		} catch(Exception e) {
			ex = e;
		}
		assertNotNull(ex);
		assertEquals("Unbalanced block CustomArtefact [Root]", ex.getMessage());
	}
	
	@Test
	public void testEmptyStack() {
		Exception ex = null;
		try {
			PlanBuilder.create().endBlock().build();
		} catch(Exception e) {
			ex = e;
		}
		assertNotNull(ex);
		assertEquals("Empty stack. Please first call startBlock before calling endBlock", ex.getMessage());
	}
	
	@Test
	public void test() {
		Plan plan =PlanBuilder.create().startBlock(artefact("Root")).endBlock().build();
		assertEquals("Root", plan.getRoot().getDescription());
	}
	
	public static AbstractArtefact artefact(String description) {
		CustomArtefact a = new CustomArtefact();
		a.setDescription(description);
		return a;
	}
	
	@Artefact(name="Custom")
	public static class CustomArtefact extends AbstractArtefact {

		@Override
		public String toString() {
			return "CustomArtefact [" + getDescription() + "]";
		}
	}
}
