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

import java.util.Map;

import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.functions.Function;
import step.functions.execution.FunctionExecutionServiceException;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;

public interface FunctionRouter {

	TokenWrapper selectToken(CallFunction callFunction, Function function, FunctionGroupContext functionGroupContext,
			Map<String, Object> bindings, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException;

}
