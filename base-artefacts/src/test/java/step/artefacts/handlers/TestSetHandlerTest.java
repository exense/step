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

import java.util.HashSet;

import org.junit.Test;

import step.artefacts.CheckArtefact;
import step.artefacts.TestSet;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;

public class TestSetHandlerTest extends AbstractArtefactHandlerTest {
	
	//@Test
	public void testParallel() throws Exception {
		HashSet<Long> threadIdSet = new HashSet<>();

		execute(threadIdSet, "10", "DummyName{childID}");
		
		assertEquals("Ensure that the TestSetHandler runs the child artefacts in parallel", 10, threadIdSet.size());
	}
	
	//@Test
	public void testSequential() throws Exception {
		HashSet<Long> threadIdSet = new HashSet<>();

		execute(threadIdSet, "1", "DummyName{childID}");
		
		assertEquals("Ensure that the TestSetHandler runs the child artefacts sequentially", 1, threadIdSet.size());
	}

	private void execute(HashSet<Long> threadIdSet, String tecExecutionThreads, String childNamePattern) {
		setupContext();
		
		context.getVariablesManager().putVariable(
				context.getReport(), "tec.execution.threads", tecExecutionThreads);
		
		context.getVariablesManager().getVariable("var");
		
		TestSet set = new TestSet();
		add(set);		
		
		int nChilds = 20;
		
		for(int j=0;j<nChilds;j++) {
			addAsChildOf(new CheckArtefact(new Runnable() {
				@Override
				public void run() {
					synchronized (threadIdSet) {
						threadIdSet.add(Thread.currentThread().getId());						
					}
					ExecutionContext.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
				}
			}), set);			
		}
		
		createSkeleton(set);
		execute(set);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		
		assertEquals(nChilds, getChildren(child).size());
		
		for(ReportNode node:getChildren(child)) {
			assertEquals(node.getStatus(), ReportNodeStatus.PASSED);					
		}
	}
}

