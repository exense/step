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
package step.core.artefacts.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static step.core.plans.builder.PlanBuilderTest.artefact;

import org.junit.Test;

import junit.framework.Assert;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;
import step.core.plans.runner.PlanRunnerResult;

public class ReportTreeVisitorTest {

	@Test
	public void testGetRootReportNode() {
		Plan plan = getDummyPlan();
		DefaultPlanRunner runner = new DefaultPlanRunner();
		PlanRunnerResult result = runner.run(plan);
		ReportTreeAccessor treeAccessor = result.getReportTreeAccessor();
		ReportTreeVisitor v = new ReportTreeVisitor(treeAccessor);
		ReportNode root = v.getRootReportNode(result.getExecutionId());

		Assert.assertNotNull(root);
		Assert.assertEquals("Root", root.getArtefactInstance().getDescription());
	}
	
	@Test
	public void test() {
		Plan plan = getDummyPlan();
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
	
	protected Plan getDummyPlan() {
		Plan plan = PlanBuilder.create().startBlock(artefact("Root"))
				.add(artefact("Node1"))
				.startBlock(artefact("Node2"))
				.add(artefact("Node2.1"))
				.endBlock()
				.endBlock().build();
		return plan;
	}
}
