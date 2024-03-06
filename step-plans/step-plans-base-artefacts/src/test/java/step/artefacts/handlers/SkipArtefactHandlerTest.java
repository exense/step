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
package step.artefacts.handlers;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.BaseArtefactPlugin;
import step.artefacts.Echo;
import step.artefacts.Sequence;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;

public class SkipArtefactHandlerTest extends AbstractArtefactHandlerTest {

	
	@Test
	public void testSkipChildOff() throws IOException {
		setupContext();
		
		// Create a sequence block with a an empty pacing value
		Sequence block = new Sequence();
		Sequence childblock = new Sequence();
		childblock.setSkipNode(new DynamicValue<Boolean>(false));
		Echo echo = new Echo();
		childblock.addChild(echo);
		echo.setText(new DynamicValue<>("'This is a test'", ""));
		
		// Create a plan with this sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(block).add(childblock).add(new Echo())
				.endBlock()
				.build();
		
		// Run the plan
		StringWriter writer = new StringWriter();
		try (ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new BaseArtefactPlugin()).build()) {
			engine.execute(plan).printTree(writer);
		}

		Assert.assertEquals("Sequence:PASSED:\n" + 
				" Sequence:PASSED:\n" +
				"  Echo:PASSED:\n" +
				" Echo:PASSED:\n" +
				"" , writer.toString());			
	}
	
	@Test
	public void testSkipChildOn() throws IOException {
		setupContext();
		
		// Create a sequence block with a an empty pacing value
		Sequence block = new Sequence();
		Sequence childblock = new Sequence();
		childblock.setSkipNode(new DynamicValue<Boolean>(true));
		Echo echo = new Echo();
		childblock.addChild(echo);
		echo.setText(new DynamicValue<>("'This is a test'", ""));
		
		// Create a plan with this sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(block).add(childblock).add(new Echo())
				.endBlock()
				.build();
		
		// Run the plan
		StringWriter writer = new StringWriter();
		try (ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new BaseArtefactPlugin()).build()) {
			engine.execute(plan).printTree(writer);
		}

		Assert.assertEquals("Sequence:PASSED:\n" +
				" Sequence:SKIPPED:\n" +
				" Echo:PASSED:\n" +
				"" , writer.toString());			
	}
	
}
