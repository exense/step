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
package step.junit5.runner;

import step.automation.packages.junit.ExcludePlans;
import step.junit.runners.annotations.ExecutionParameters;
import step.junit.runners.annotations.Plans;

@ExcludePlans({"JMeter Plan", "testAutomation.plan"})
@Plans({"plans/plan2.plan", "plans/plan3.plan", "plans/assertsTest.plan"})
@ExecutionParameters({"PARAM_EXEC","Value","PARAM_EXEC2","Value","PARAM_EXEC3","Value"})
public class StepRunnerWithPlansAnnotationTest extends StepJUnit5 {

}
