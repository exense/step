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

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.CheckArtefact;
import step.artefacts.IfBlock;
import step.artefacts.RetryIfFails;
import step.artefacts.Set;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;

public class RetryIfFailsHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testSuccess() {
		setupContext();
		
		RetryIfFails block = new RetryIfFails();
		block.setMaxRetries(new DynamicValue<Integer>(2));
		
		CheckArtefact check1 = new CheckArtefact(c->context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED));
		block.addChild(check1);
		
		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		
		assertEquals(1, getChildren(child).size());
	}
	
	@Test
	public void testMaxRetry() {
		setupContext();
		
		RetryIfFails block = new RetryIfFails();
		block.setMaxRetries(new DynamicValue<Integer>(2));
		block.setGracePeriod(new DynamicValue<Integer>(1000));
		
		CheckArtefact check1 = new CheckArtefact(c->context.getCurrentReportNode().setStatus(ReportNodeStatus.FAILED));
		block.addChild(check1);		
		
		execute(block);
		
		ReportNode child = getFirstReportNode();
		Assert.assertTrue(child.getDuration()>=2000);
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		
		assertEquals(2, getChildren(child).size());
	}
	
	@Test
	public void testTimeout() {
		setupContext();
		
		RetryIfFails block = new RetryIfFails();
		block.setTimeout(new DynamicValue<Integer>(200));
		block.setGracePeriod(new DynamicValue<Integer>(50));
		
		CheckArtefact check1 = new CheckArtefact(c-> {
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		});
		block.addChild(check1);		
		
		execute(block);
		
		ReportNode child = getFirstReportNode();
		Assert.assertTrue(child.getDuration()>=250);
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
	}
	
	@Test
	public void testFalse() {
		setupContext();
		
		IfBlock block = new IfBlock("false");
		block.addChild(new Set());

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);	
		assertEquals(0, getChildren(child).size());
	}
}

