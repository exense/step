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
package step.functions.execution;

import java.util.Map;

import step.core.AbstractStepContext;
import step.functions.Function;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Interest;

public interface FunctionExecutionService {

	void registerTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor);

	void unregisterTokenLifecycleInterceptor(TokenLifecycleInterceptor interceptor);

	TokenWrapper getLocalTokenHandle();

	TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession, TokenWrapperOwner tokenWrapperOwner, boolean skipAutoProvisioning) throws FunctionExecutionServiceException;
	
	void returnTokenHandle(String tokenHandleId) throws FunctionExecutionServiceException;

	/**
	 * Call function from while running the execution
	 */
	default <IN,OUT> Output<OUT> callFunction(String tokenHandleId, Function function, FunctionInput<IN> functionInput, Class<OUT> outputClass, AbstractStepContext executionContext){
		return callFunction(tokenHandleId, function, functionInput, outputClass);
	}

	<IN,OUT> Output<OUT> callFunction(String tokenHandleId, Function function, FunctionInput<IN> functionInput, Class<OUT> outputClass);
}
