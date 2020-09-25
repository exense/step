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
package step.functions.manager;

import java.util.Map;

import step.functions.Function;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;

public interface FunctionManager {

	Function saveFunction(Function function) throws SetupFunctionException, FunctionTypeException;

	Function copyFunction(String functionId) throws FunctionTypeException;

	void deleteFunction(String functionId) throws FunctionTypeException;

	Function newFunction(String functionType);
	
	Function getFunctionByAttributes(Map<String, String> attributes);
	
	Function getFunctionById(String id);

}
