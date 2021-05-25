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

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.Check;
import step.artefacts.TestScenario;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.threadpool.ThreadPoolPlugin;

public class TestScenarioHandlerTest {
	
	@Test
	public void test() throws IOException {
		Plan plan = PlanBuilder.create().startBlock(new TestScenario()).add(passedCheck()).add(passedCheck()).endBlock().build();
		
		StringWriter writer = new StringWriter();
		ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).build();
		engine.execute(plan).printTree(writer);
		
		Assert.assertTrue(writer.toString().startsWith("TestScenario:"+ReportNodeStatus.PASSED));
	}
	
	private Check passedCheck() {
		Check passedCheck = new Check();
		passedCheck.setExpression(new DynamicValue<Boolean>(true));
		return passedCheck;
	}

}
