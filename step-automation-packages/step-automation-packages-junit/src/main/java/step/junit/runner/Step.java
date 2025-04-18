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
package step.junit.runner;

import org.junit.runners.model.InitializationError;
import step.automation.packages.junit.*;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.engine.plugins.AbstractExecutionEnginePlugin;

public class Step extends AbstractStepRunner {

	public Step(Class<?> klass) throws InitializationError {
		super(klass, klass);

		try {
			executionEngine = ExecutionEngine.builder().withPlugin(new AbstractExecutionEnginePlugin() {
				@Override
				public void afterExecutionEnd(ExecutionContext context) {
					resourceManager = context.getResourceManager();
				}
			}).withPluginsFromClasspath().build();

			listPlans = new JUnitPlansProvider(klass).getTestPlans(executionEngine);
		} catch (Exception e) {
			throw new InitializationError(e);
		}
	}

}
