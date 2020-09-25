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
package step.core.plans.runner;

import java.util.Map;

import step.core.plans.Plan;

public interface PlanRunner {

	/**
	 * Runs a plan instance
	 * 
	 * @param plan the plan to be run
	 * @return an handle to the execution result
	 */
	public PlanRunnerResult run(Plan plan);
	
	/**
	 * Runs a plan instance using the provided execution parameters
	 * 
	 * @param plan the plan to be run
	 * @param executionParameters the execution parameters to be used for the execution. 
	 * These parameters are equivalent to the parameters selected on the execution screen of the STEP UI
	 * @return  an handle to the execution result
	 */
	public PlanRunnerResult run(Plan plan, Map<String, String> executionParameters);
}
