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
package step.client.package2;

import org.junit.runner.RunWith;

import step.client.StepRunnerWithPlansAnnotationTest;
import step.junit.runner.Step;
import step.junit.runners.annotations.ExecutionParameters;
import step.junit.runners.annotations.Plan;

/**
 * Running this class in JUnit with the {@link Step} should
 * execute the plans located in the same package of this class
 * i.e plan3.plan and the methods annotated by Plan
 * 
 * The other plans (plan1 and plan2) shouldn't be executed. This
 * is asserted by the fact that plan2.plan fails if executed. 
 * 
 * Methods annotated by {@link Plan} from other classes shouldn't 
 * be executed. This is asserted indirectly by the fact that the method
 * {@link StepRunnerWithPlansAnnotationTest#plan2()} requires the {@link ExecutionParameters} 
 * PARAM_EXEC which isn't set by this class
 */
@RunWith(Step.class)
public class StepRunnerWithPlanAnnotationTest {
	
	/**
	 * This creates and run the plan inlined in the annotation
	 */
	@Plan("For 1 to 10\n callExisting\n End")
	public void myPlan1() {}
	
	/**
	 * This creates a Plan calling the Keyword named after this method
	 */
	@Plan()
	public void callExisting() {}

}
