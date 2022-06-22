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
package step.client.functions;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.functions.services.GetTokenHandleParameter;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Interest;

public class RemoteFunctionExecutionService extends AbstractRemoteClient implements FunctionExecutionService {

	public RemoteFunctionExecutionService(ControllerCredentials credentials){
		super(credentials);
	}
	
	public RemoteFunctionExecutionService(){
		super();
	}
	
	@Override
	public TokenWrapper getLocalTokenHandle() {
		GetTokenHandleParameter parameter = new GetTokenHandleParameter();
		parameter.setLocal(true);
		
		Builder b = requestBuilder("/rest/functions/executor/tokens/select");
		Entity<GetTokenHandleParameter> entity = Entity.entity(parameter, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity,TokenWrapper.class));
	}

	@Override
	public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession, TokenWrapperOwner tokenWrapperOwner) {
		GetTokenHandleParameter parameter = new GetTokenHandleParameter();
		parameter.setAttributes(attributes);
		parameter.setInterests(interests);
		parameter.setCreateSession(createSession);
		
		Builder b = requestBuilder("/rest/functions/executor/tokens/select");
		Entity<GetTokenHandleParameter> entity = Entity.entity(parameter, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity,TokenWrapper.class));
	}

	@Override
	public void returnTokenHandle(String tokenId) {
		Builder b = requestBuilder("/rest/functions/executor/tokens/"+tokenId+"/return");
		executeRequest(()->b.post(Entity.json(null)));
	}

	@Override
	public <IN, OUT> Output<OUT> callFunction(String tokenId, Function function, FunctionInput<IN> input,
			Class<OUT> outputClass) {
		Builder b = requestBuilder("/rest/functions/executor/tokens/"+tokenId+"/execute/"+function.getId().toString());
		Entity<FunctionInput<IN>> entity = Entity.entity(input, MediaType.APPLICATION_JSON);
		
		ParameterizedType parameterizedGenericType = new ParameterizedType() {
	        public Type[] getActualTypeArguments() {
	            return new Type[] { outputClass };
	        }

	        public Type getRawType() {
	            return Output.class;
	        }

	        public Type getOwnerType() {
	            return Output.class;
	        }
	    };

	    GenericType<Output<OUT>> genericType = new GenericType<Output<OUT>>(
	            parameterizedGenericType) {
	    };
		
		return executeRequest(()->b.post(entity,genericType));
	}
}
