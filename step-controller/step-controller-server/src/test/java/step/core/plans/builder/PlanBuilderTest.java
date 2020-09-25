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
package step.core.plans.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.CheckArtefact;
import step.core.plans.Plan;

public class PlanBuilderTest {

	@Test
	public void testNoRoot() {
		Exception ex = null;
		try {
			PlanBuilder.create().add(artefact("Root"));
		} catch(Exception e) {
			ex = e;
		}
		assertNotNull(ex);
		assertEquals("No root artefact defined. Please first call the method startBlock to define the root element", ex.getMessage());
	}
	
	@Test
	public void testUnablancedBlock() {
		Exception ex = null;
		try {
			PlanBuilder.create().startBlock(artefact("Root")).build();
		} catch(Exception e) {
			ex = e;
		}
		assertNotNull(ex);
		assertEquals("Unbalanced block CheckArtefact [Root]", ex.getMessage());
	}
	
	@Test
	public void testEmptyStack() {
		Exception ex = null;
		try {
			PlanBuilder.create().endBlock().build();
		} catch(Exception e) {
			ex = e;
		}
		assertNotNull(ex);
		assertEquals("Empty stack. Please first call startBlock before calling endBlock", ex.getMessage());
	}
	
	@Test
	public void test() {
		Plan plan =PlanBuilder.create().startBlock(artefact("Root")).endBlock().build();
		assertEquals("Root", plan.getRoot().getDescription());
	}
	
	public static AbstractArtefact artefact(String description) {
		CheckArtefact a = new CheckArtefact();
		a.setDescription(description);
		return a;
	}
}
