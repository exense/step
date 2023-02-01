/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.artefacts.handlers;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.CallPlan;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;

public class CallPlanHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test() {
		context = newExecutionContext();
		
		AtomicBoolean executed = new AtomicBoolean();
		
		CheckArtefact check = new CheckArtefact(c->{
			c.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
			executed.set(true);
		});
		
		
		Plan calledPlan = PlanBuilder.create().startBlock(check).endBlock().build();
		context.getPlanAccessor().save(calledPlan);
		
		CallPlan callPlan = new CallPlan();
		callPlan.setPlanId(calledPlan.getId().toString());
		Plan plan = PlanBuilder.create().startBlock(callPlan).endBlock().build();
		context.getPlanAccessor().save(plan);
		
		createSkeleton(plan.getRoot());
		execute(plan.getRoot());	
		
		Assert.assertTrue(executed.get());
		Assert.assertTrue(getChildren(context.getReport()).get(0).getStatus().equals(ReportNodeStatus.PASSED));
	}

}
