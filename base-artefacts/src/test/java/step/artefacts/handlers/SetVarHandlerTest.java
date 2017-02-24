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
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.variables.VariablesManager;

public class SetVarHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test() {
		setupContext();
		
		Set set = add(new Set());
		set.setKey(new DynamicValue<String>("var"));
		set.setValue(new DynamicValue<String>("val1"));

		execute(set);
		
		VariablesManager v = ExecutionContext.getCurrentContext().getVariablesManager();
		
		assertEquals("val1",v.getVariable("var"));

		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
	}
}

