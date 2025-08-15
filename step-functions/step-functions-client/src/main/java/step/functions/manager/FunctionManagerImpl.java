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

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.SetupFunctionException;
import step.handlers.javahandler.Keyword;

import java.util.HashMap;
import java.util.Map;

public class FunctionManagerImpl implements FunctionManager {

	private static final Logger logger = LoggerFactory.getLogger(FunctionManagerImpl.class);

	private final FunctionAccessor functionRepository;
	
	private final FunctionTypeRegistry functionTypeRegistry;

	public FunctionManagerImpl(FunctionAccessor functionRepository, FunctionTypeRegistry functionTypeRegistry) {
		super();
		this.functionRepository = functionRepository;
		this.functionTypeRegistry = functionTypeRegistry;
	}
	
	@Override
	public Function saveFunction(Function function) throws SetupFunctionException, FunctionTypeException {
		if(function.getId()==null || functionRepository.get(function.getId())==null) {
			setupFunction(function);
		} else {
			updateFunction(function);
		}
		return function;
	}

	private void setupFunction(Function function) throws SetupFunctionException {
		AbstractFunctionType<Function> type = getFunctionType(function);
		if (type == null) {
			throw new SetupFunctionException("Function type is not resolved for " + function.getClass());
		}
		type.setupFunction(function);
		functionRepository.save(function);
	}
	
	private Function updateFunction(Function function) throws FunctionTypeException {
		AbstractFunctionType<Function> type = getFunctionType(function);
		function = type.updateFunction(function);
		functionRepository.save(function);
		return function;
	}

	@Override
	public Function copyFunction(String functionId) throws FunctionTypeException {
		Function source = functionRepository.get(new ObjectId(functionId));
		if(source!=null) {
			AbstractFunctionType<Function> type = getFunctionType(source);
			Function target = type.copyFunction(source);
			functionRepository.save(target);
			return target;
		} else {
			return null;
		}
		
	}
	
	
	@Override
	public void deleteFunction(String functionId) throws FunctionTypeException {
		Function function = functionRepository.get(new ObjectId(functionId));
		AbstractFunctionType<Function> type = getFunctionType(function);
		type.deleteFunction(function);
		functionRepository.remove(function.getId());
	}
	
	@Override
	public Function newFunction(String functionType) {
		return functionTypeRegistry.getFunctionType(functionType).newFunction();
	}

	@Override
	public Function newFunction(String functionType, Map<String, String> configuration) {
		return functionTypeRegistry.getFunctionType(functionType).newFunction(configuration);
	}

	protected AbstractFunctionType<Function> getFunctionType(Function function) {
		return functionTypeRegistry.getFunctionTypeByFunction(function);
	}

	@Override
	public Function getFunctionByAttributes(Map<String, String> attributes) {
		return functionRepository.findByAttributes(attributes);
	}

	@Override
	public Function getFunctionById(String id) {
		return functionRepository.get(new ObjectId(id));
	}

	public static void applyRoutingFromAnnotation(Function function, Keyword annotation) {
		String[] routing = annotation.routing();
		if (routing == null || routing.length == 0){
			//Default routing
			return;
		} else if (routing.length == 1) {
			if (routing[0].equals(Keyword.EXECUTE_ON_CONTROLLER)) {
				function.setExecuteLocally(true);
			} else {
				throw new IllegalArgumentException("Invalid routing value: '" + routing[0] + "'. " +
						"If a single value is provided, it must be the reserved keyword 'controller'.");
			}
		} else if (routing.length % 2 != 0) {
			throw new IllegalArgumentException("Invalid routing array length: " + routing.length + ". " +
					"When specifying agent selection criteria as key-value pairs, " +
					"the array must contain an even number of elements (key1, value1, key2, value2, ...).");
		} else {
			Map<String, String> map = new HashMap<>(); // preserves order
			for (int i = 0; i < routing.length; i += 2) {
				String key = routing[i];
				String value = routing[i + 1];
				map.put(key, value);
			}
			function.setTokenSelectionCriteria(map);
		}
	}
	
}
