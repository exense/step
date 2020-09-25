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

import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import step.client.credentials.ControllerCredentials;
import step.functions.Function;
import step.functions.type.FunctionTypeException;

public class RemoteFunctionManagerImpl extends RemoteFunctionManager {

	public RemoteFunctionManagerImpl(ControllerCredentials credentials){
		super(credentials);
	}

	public RemoteFunctionManagerImpl(){
		super();
	}

	@Override
	public Function getFunctionByAttributes(Map<String, String> attributes) {
		Builder b = requestBuilder("/rest/functions/search");
		Entity<Map<String, String>> entity = Entity.entity(attributes, MediaType.APPLICATION_JSON);
		return b.post(entity,Function.class);
	}

	@Override
	public Function getFunctionById(String id) {
		Builder b = requestBuilder("/rest/functions/"+id);
		return executeRequest(()-> b.get(Function.class));
	}

	@Override
	public Function saveFunction(Function function) {
		Builder b = requestBuilder("/rest/functions");
		Entity<Function> entity = Entity.entity(function, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity, Function.class));		
	}

	@Override
	public void deleteFunction(String functionId) {
		Builder b = requestBuilder("/rest/functions/"+functionId);
		executeRequest(()->b.delete());
	}

	@Override
	public Function copyFunction(String functionId) throws FunctionTypeException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Function newFunction(String functionType) {
		throw new RuntimeException("Not implemented");
	}
}
