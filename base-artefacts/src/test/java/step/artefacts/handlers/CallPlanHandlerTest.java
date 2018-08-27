package step.artefacts.handlers;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.CallPlan;
import step.artefacts.CheckArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ContextBuilder;
import step.core.plans.Plan;
import step.planbuilder.PlanBuilder;

public class CallPlanHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test() {
		context = ContextBuilder.createLocalExecutionContext();
		
		AtomicBoolean executed = new AtomicBoolean();
		
		CheckArtefact check = new CheckArtefact(c->{
			context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
			executed.set(true);
		});
		
		
		Plan calledPlan = PlanBuilder.create().add(check).build();
		context.getGlobalContext().getArtefactAccessor().save(new ArrayList<>(calledPlan.getArtefacts()));
		
		CallPlan callPlan = new CallPlan();
		callPlan.setArtefactId(calledPlan.getRoot().getId().toString());
		Plan plan = PlanBuilder.create().add(callPlan).build();
		context.getGlobalContext().getArtefactAccessor().save(new ArrayList<>(plan.getArtefacts()));
		
		ArtefactHandler.delegateCreateReportSkeleton(context, plan.getRoot(),context.getReport());
		ArtefactHandler.delegateExecute(context, plan.getRoot(),context.getReport());	
		
		Assert.assertTrue(executed.get());
		Assert.assertTrue(getChildren(context.getReport()).get(0).getStatus().equals(ReportNodeStatus.PASSED));
	}

}
