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
package step.artefacts;

import org.junit.After;
import org.junit.Before;
import step.artefacts.handlers.functions.TokenAutoscalingExecutionPlugin;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.OperationMode;
import step.engine.plugins.FunctionPlugin;
import step.threadpool.ThreadPoolPlugin;

public class AbstractArtefactTest {

	protected ExecutionEngine executionEngine;

	public AbstractArtefactTest() {
		super();
	}

	@Before
	public void beforeTest() {
		executionEngine = ExecutionEngine.builder().withPlugin(new ThreadPoolPlugin())
				.withPlugin(new BaseArtefactPlugin()).withPlugin(new TokenAutoscalingExecutionPlugin())
				.withPlugin(new FunctionPlugin()).withOperationMode(OperationMode.CONTROLLER).build();
	}

	@After
	public void afterTest() {
		// cleans up potential leftovers. try-with-resources is not possible because of scoping and class inheritance.
		executionEngine.close();
	}

	protected ExecutionContext newExecutionContext() {
		return executionEngine.newExecutionContext();
	}

}
