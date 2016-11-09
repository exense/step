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

import org.junit.Test;

import step.artefacts.Sequence;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class SequenceHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test1ChildPassed() {		
		for(ReportNodeStatus status:ReportNodeStatus.values()) {
			test1Child(status);			
		};
	}

	private void test1Child(ReportNodeStatus status) {
		setupContext();
		
		Sequence block = add(new Sequence());
		addAsChildOf(newTestArtefact(status), block);

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(status, child.getStatus());
	}
	
	@Test
	public void test2ChildrenFailed() {
		setupContext();
		
		Sequence block = add(new Sequence());
		addAsChildOf(newTestArtefact(ReportNodeStatus.PASSED), block);
		addAsChildOf(newTestArtefact(ReportNodeStatus.FAILED), block);

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(ReportNodeStatus.FAILED, child.getStatus());
	}
	
	@Test
	public void test2ChildrenTechError() {
		setupContext();
		
		Sequence block = add(new Sequence());
		addAsChildOf(newTestArtefact(ReportNodeStatus.PASSED), block);
		addAsChildOf(newTestArtefact(ReportNodeStatus.TECHNICAL_ERROR), block);

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(ReportNodeStatus.TECHNICAL_ERROR, child.getStatus());
	}
	
	@Test
	public void testDefaultContinueOnError() {
		setupContext();
		
		Sequence block = add(new Sequence());
		addAsChildOf(newTestArtefact(ReportNodeStatus.TECHNICAL_ERROR), block);
		addAsChildOf(newTestArtefact(ReportNodeStatus.PASSED), block);

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(ReportNodeStatus.TECHNICAL_ERROR, child.getStatus());
		assertEquals(1, getChildren(child).size());
	}
	
	@Test
	public void testDefaultContinueOnError_2() {
		setupContext();
		
		Sequence block = add(new Sequence());
		addAsChildOf(newTestArtefact(ReportNodeStatus.PASSED), block);
		addAsChildOf(newTestArtefact(ReportNodeStatus.TECHNICAL_ERROR), block);

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(ReportNodeStatus.TECHNICAL_ERROR, child.getStatus());
		assertEquals(2, getChildren(child).size());
	}
	
}

