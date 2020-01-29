package step.artefacts.handlers;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.CallPlan;
import step.artefacts.CheckArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ContextBuilder;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;

public class CallPlanHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test() {
		context = ContextBuilder.createLocalExecutionContext();
		
		AtomicBoolean executed = new AtomicBoolean();
		
		CheckArtefact check = new CheckArtefact(c->{
			context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
			executed.set(true);
		});
		
		
		Plan calledPlan = PlanBuilder.create().startBlock(check).endBlock().build();
		context.getPlanAccessor().save(calledPlan);
		
		CallPlan callPlan = new CallPlan();
		callPlan.setPlanId(calledPlan.getId().toString());
		Plan plan = PlanBuilder.create().startBlock(callPlan).endBlock().build();
		context.getPlanAccessor().save(plan);
		
		ArtefactHandler.delegateCreateReportSkeleton(context, plan.getRoot(),context.getReport());
		ArtefactHandler.delegateExecute(context, plan.getRoot(),context.getReport());	
		
		Assert.assertTrue(executed.get());
		Assert.assertTrue(getChildren(context.getReport()).get(0).getStatus().equals(ReportNodeStatus.PASSED));
	}

}
