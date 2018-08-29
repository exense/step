package step.core.artefacts.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;
import step.core.plans.runner.PlanRunnerResult;

public class ReportTreeVisitorTest {

	@Test
	public void test() {
		Plan plan = PlanBuilder.create().startBlock(artefact("Root"))
											.add(artefact("Node1"))
											.startBlock(artefact("Node2"))
												.add(artefact("Node2.1"))
											.endBlock()
										.endBlock().build();
		
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		PlanRunnerResult result = runner.run(plan);
		ReportTreeAccessor treeAccessor = result.getReportTreeAccessor();
		ReportTreeVisitor v = new ReportTreeVisitor(treeAccessor);
		
		v.visit(result.getExecutionId(), e->{
			ReportNode node = e.getNode();
			if(node.getArtefactInstance().getDescription().equals("Root")) {
				assertNull(e.getParentNode());
				assertEquals(0, e.getStack().size());
			} else if(node.getArtefactInstance().getDescription().equals("Node1")) {
				assertEquals("Root",e.getParentNode().getArtefactInstance().getDescription());
				assertEquals(1, e.getStack().size());
			} else if(node.getArtefactInstance().getDescription().equals("Node2")) {
				assertEquals("Root",e.getParentNode().getArtefactInstance().getDescription());
				assertEquals(1, e.getStack().size());
			} else if(node.getArtefactInstance().getDescription().equals("Node2.1")) {
				assertEquals("Node2",e.getParentNode().getArtefactInstance().getDescription());
				assertEquals(2, e.getStack().size());
				assertEquals("Root",e.getRootNode().getArtefactInstance().getDescription());
			} else {
				throw new RuntimeException("Unexpected node "+node.getName());
			}
		});
	}

	protected AbstractArtefact artefact(String description) {
		CustomArtefact a = new CustomArtefact();
		a.setDescription(description);
		return a;
	}
	
	@Artefact(name="Custom", handler = CustomHandler.class)
	public static class CustomArtefact extends AbstractArtefact {
		
	}
	
	public static class CustomHandler extends ArtefactHandler<CustomArtefact, ReportNode>{

		@Override
		protected void createReportSkeleton_(ReportNode parentNode, CustomArtefact testArtefact) {
			
		}

		@Override
		protected void execute_(ReportNode node, CustomArtefact testArtefact) throws Exception {
			testArtefact.getChildrenIDs().forEach(childId->{
				ArtefactHandler.delegateExecute(context, context.getArtefactAccessor().get(childId), node);
			});
		}

		@Override
		public ReportNode createReportNode_(ReportNode parentNode, CustomArtefact testArtefact) {
			return new ReportNode();
		}
		
	}
	
	

}
