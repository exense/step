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

import step.artefacts.Set;
import step.artefacts.While;
import step.artefacts.testutils.AbstractArtefactHandlerTest;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;

public class WhileHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testTrue() {
		setupContext();
		
		While block = new While("true");
		block.setMaxIterations(new DynamicValue<>(2));
		block = add(block);
		Set set = addAsChildOf(new Set(), block);

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		
		ReportNode sequence = getChildren(child).get(1);
		ReportNode setNode =  getChildren(sequence).get(0);
		assertEquals(set.getId(), setNode.getArtefactID());
		assertEquals(setNode.getStatus(), ReportNodeStatus.PASSED);
	}
	
	@Test
	public void testFalse() {
		setupContext();
		
		While block = new While("false");
		block.setMaxIterations(new DynamicValue<>(1));
		block = add(block);

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);	
		assertEquals(0, getChildren(child).size());
	}
}

