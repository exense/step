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
package step.client;

import org.junit.Assert;
import org.junit.runner.RunWith;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.junit.runner.Step;
import step.junit.runners.annotations.ExecutionParameters;
import step.junit.runners.annotations.Plan;
import step.junit.runners.annotations.Plans;

@RunWith(Step.class)
@Plans({"plans/plan2.plan", "plans/plan3.plan", "plans/assertsTest.plan"})
@ExecutionParameters({"PARAM_EXEC","Value","PARAM_EXEC2","Value","PARAM_EXEC3","Value"})
public class StepRunnerWithPlansAnnotationTest extends AbstractKeyword {

	public void implicitPlanWithDefaultKeywordName() {
	}
	@Plan
	@Keyword(name = "My custom keyword name")
	public void implicitPlanWithWithCustomKeywordName() {}
	
//	Negative test: Commented out as this test is failing per design. 
// 	Uncomment it out to perform the negative test manually
//	@Plan
//	public void implicitPlanWithWithoutKeywordAnnotation() {}

	//Negative test: Commented out as this test is failing per design.
	//@Plan
	@Keyword()
	public void implicitPlanWithError() {
		throw new RuntimeException();
	}

	@Plan("planWithAssert\nAssert key = \"value\"")
	@Keyword
	public void planWithAssert() {
		output.add("key","value");
		output.add("intKey", 77);
		output.add("boolKey", true);
	}

	@Plan
	@Keyword
	public void explicitPlanWithExecutionParameter() {
		Assert.assertEquals("Value", properties.get("PARAM_EXEC"));
	}

	// This test has to be executed with -DPARAM_EXEC2=Sysprop1 as system property
	// It is failing otherwise and therefore commented out
	//@Plan
	@Keyword
	public void explicitPlanWithSystemProperty() {
		Assert.assertEquals("Sysprop1", properties.get("PARAM_EXEC2"));
	}

	// This test has to be executed with STEP_PARAM_EXEC3=Envvar1 as environment variable
	// It is failing otherwise and therefore commented out
	//@Plan
	@Keyword
	public void explicitPlanWithEnvironmentVariable() {
		Assert.assertEquals("Envvar1", properties.get("PARAM_EXEC3"));
	}

	@Plan("Echo PARAM_EXEC")
	@Keyword(name = "Inline Plan")
	public void inlinePlan() {}

}
