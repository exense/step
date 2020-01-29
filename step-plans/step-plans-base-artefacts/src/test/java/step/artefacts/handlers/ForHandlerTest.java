/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.CheckArtefact;
import step.artefacts.ForBlock;
import step.artefacts.reports.ForBlockReportNode;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;
import step.datapool.sequence.IntSequenceDataPool;

public class ForHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testSuccess() {
		setupContext();
		
		context.getVariablesManager().putVariable(
				context.getReport(), "var", "val1");
			
		ForBlock f = new ForBlock();
		
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setEnd(new DynamicValue<Integer>(3));;
		conf.setInc(new DynamicValue<Integer>(2));;
		
		f.setDataSource(conf);
		f.setItem(new DynamicValue<String>("item"));
		
		AtomicInteger i = new AtomicInteger(1);
		
		CheckArtefact check1 = new CheckArtefact(c->{
				context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
				assertEquals(i.get(),(int)context.getVariablesManager().getVariableAsInteger("item"));
				i.addAndGet(2);
			});
		
		f.addChild(check1);
		
		execute(f);
		
		ForBlockReportNode child = (ForBlockReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals(2, child.getCount());
		assertEquals(0, child.getErrorCount());

		
		assertEquals(getChildren(child).size(), 2);
		
		for(ReportNode node:getChildren(child)) {
			assertEquals(node.getStatus(), ReportNodeStatus.PASSED);		
			assertEquals(getChildren(node).size(), 1);
			assertEquals(check1.getId(),getChildren(node).get(0).getArtefactID());
			
		}
		
	}
	
	@Test
	public void testBreak() {
		setupContext();
			
		ForBlock f = new ForBlock();
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setEnd(new DynamicValue<Integer>(10));;
		
		f.setDataSource(conf);
		
		AtomicInteger i = new AtomicInteger(1);
		
		CheckArtefact check1 = new CheckArtefact(c-> {
				if(i.get()==2) {
					context.getVariablesManager().updateVariable("break", "true");
				}
				i.addAndGet(1);
				context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
			});
		
		f.addChild(check1);
		execute(f);
		
		ForBlockReportNode child = (ForBlockReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		assertEquals(2, child.getCount());
		assertEquals(0, child.getErrorCount());		
	}
	
	@Test
	public void testMaxFailedCount() {
		setupContext();
			
		ForBlock f = new ForBlock();
		
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setEnd(new DynamicValue<Integer>(10));;
		
		f.setDataSource(conf);
		f.setMaxFailedLoops(new DynamicValue<Integer>(2));
		
		AtomicInteger i = new AtomicInteger(1);
		
		CheckArtefact check1 = new CheckArtefact(c -> {
				context.getCurrentReportNode().setStatus(ReportNodeStatus.FAILED);
			});
		
		f.addChild(check1);
		
		execute(f);
		
		ForBlockReportNode child = (ForBlockReportNode) getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals(2, child.getCount());
		assertEquals(2, child.getErrorCount());		
	}
	
	@Test
	public void testTechnicalError() throws IOException, TimeoutException, InterruptedException {
		ForBlock forBlock = new ForBlock();

		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setStart(new DynamicValue<Integer>(1));;
		conf.setEnd(new DynamicValue<Integer>(2));;
		
		forBlock.setDataSource(conf);
		
		
		Plan plan = PlanBuilder.create().startBlock(forBlock).add(new CheckArtefact(c -> {
				c.getCurrentReportNode().setStatus(ReportNodeStatus.TECHNICAL_ERROR);
			})).endBlock().build();
		
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		runner.run(plan).waitForExecutionToTerminate().visitReportTree(e->{
			// Root node
			if(e.getParentNode()==null) {
				Assert.assertEquals(ForBlockReportNode.class, e.getNode().getClass());;
				// Assert that the status of the root node is TECHNICAL_ERROR
				Assert.assertEquals(ReportNodeStatus.TECHNICAL_ERROR, e.getNode().getStatus());
			}
		});
		
	}
}

