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

import java.util.List;

import org.junit.Test;

import step.artefacts.Check;
import step.artefacts.Groovy;
import step.artefacts.Sequence;
import step.artefacts.Set;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;

public class GroovyHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test() {
		setupContext();
		
		Sequence s = add(new Sequence());
		
		Set set1 = new Set();
		set1.setKey(new DynamicValue<String>("var1"));
		set1.setValue(new DynamicValue<>("[:]",""));
		addAsChildOf(set1,s);

		
		Groovy g = new Groovy();
		g.setExpression("var1['test']='test'");
		addAsChildOf(g, s);
		
		Check c = new Check();
		c.setExpression("var1['test']=='test'");
		addAsChildOf(c	, s);

		execute(s);

		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		
		List<ReportNode> children = getChildren(child);
		assertEquals(3, children.size());
		for(ReportNode child1:children) {
			assertEquals(child1.getStatus(), ReportNodeStatus.PASSED);
		}
			
	}
}

