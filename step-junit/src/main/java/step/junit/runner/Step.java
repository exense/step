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

import java.util.List;

import org.junit.experimental.categories.Categories;
import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.resources.ResourceManager;

public class Step extends Categories {

	private final StepClassParser classParser = new StepClassParser(false);
	private final Class<?> klass;
	private final List<Runner> listRunners;

	private ExecutionEngine executionEngine;

	public Step(Class<?> klass, RunnerBuilder builder) throws InitializationError {
		super(klass,builder);
		this.klass = klass;
		try {
			executionEngine = ExecutionEngine.builder().withPluginsFromClasspath().build();
			listRunners = classParser.createRunnersForClass(klass,executionEngine);
		} catch (Exception e) {
			throw new InitializationError(e);
		}
	}

	@Override
	protected List<Runner> getChildren() {
		return listRunners;
	}
}
