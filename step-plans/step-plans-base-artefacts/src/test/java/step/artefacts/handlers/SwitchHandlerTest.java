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

import step.artefacts.Case;
import step.artefacts.IfBlock;
import step.artefacts.Set;
import step.artefacts.Switch;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;

public class SwitchHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testTrue() {
		setupContext();
		
		context.getVariablesManager().putVariable(
				context.getReport(), "var", "val1");
		
		context.getVariablesManager().getVariable("var");
		
		Switch select = new Switch();
		select.setExpression(new DynamicValue<>("'val1'", ""));
		
		Case c1 = new Case();
		c1.setValue(new DynamicValue<String>("val1"));
		select.addChild(c1);
		
		Set set1 = new Set();
		c1.addChild(set1);
		
		Case c2 = new Case();
		c2.setValue(new DynamicValue<String>("val2"));
		select.addChild(c2);
		
		Set set2 = new Set();
		c2.addChild(set2);
		
		execute(select);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		
		assertEquals(getChildren(child).size(), 1);
		
		ReportNode case1 = getChildren(child).get(0);
		assertEquals(c1.getId(), case1.getArtefactID());
		assertEquals(case1.getStatus(), ReportNodeStatus.PASSED);
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

