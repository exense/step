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

import step.junit.runner.Step;
import step.junit.runners.annotations.ExecutionParameters;

/**
 * Running this class in JUnit with the {@link Step} should
 * only execute the plans located in the same package of this class
 * i.e plan3.plan
 * 
 * The other plans (plan1 and plan2) shouldn't be executed. This
 * is asserted by the fact that plan2.plan fails if executed. 
 */
@RunWith(Step.class)
@ExecutionParameters({"PARAM_EXEC","Value"})
public class StepRunnerTest {

}
