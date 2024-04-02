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

import java.util.HashMap;
import java.util.Map;

import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;

/**
 * A simple runner that runs plans locally and doesn't support functions
 * 
 * @deprecated Use {@link ExecutionEngine} instead
 * @author Jérôme Comte
 *
 */
public class DefaultPlanRunner implements PlanRunner {

	private final ExecutionEngine engine = ExecutionEngine.builder().withPluginsFromClasspath().build();
	protected Map<String, String> properties;
	
	public DefaultPlanRunner() {
		this(null);
	}
	
	public DefaultPlanRunner(Map<String, String> properties) {
		super();
		this.properties = properties;
	}

	@Override
	public PlanRunnerResult run(Plan plan) {
		return run(plan, null);
	}

	@Override
	public PlanRunnerResult run(Plan plan, Map<String, String> executionParameters) {
		Map<String, String> mergedExecutionParameters = new HashMap<>();
		if(properties != null) {
			mergedExecutionParameters.putAll(properties);
		}
		if(executionParameters != null) {
			mergedExecutionParameters.putAll(executionParameters);
		}
		return engine.execute(plan, mergedExecutionParameters);
	}

	@Override
	public void close() {
		engine.close();
	}
}
