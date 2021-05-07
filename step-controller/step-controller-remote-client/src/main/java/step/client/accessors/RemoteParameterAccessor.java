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
package step.client.accessors;

import step.client.collections.remote.RemoteCollectionFactory;
import step.client.credentials.ControllerCredentials;
import step.core.accessors.AbstractAccessor;
import step.core.plans.Plan;
import step.parameter.Parameter;
import step.parameter.ParameterAccessor;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteParameterAccessor extends AbstractAccessor<Parameter> implements ParameterAccessor {

	public RemoteParameterAccessor(RemoteCollectionFactory remoteCollectionFactory) {
		super(remoteCollectionFactory.getCollection( "parameters", Parameter.class));
	}

	/*
	@Override
	public List<Parameter> getRange(int skip, int limit) {
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("skip", Integer.toString(skip));
		queryParams.put("limit", Integer.toString(limit));
		GenericType<List<Parameter>> genericEntity = new GenericType<List<Parameter>>(
				parameterizedGenericType) {
		};
		Invocation.Builder b = requestBuilder(path+"all", queryParams);
		return executeRequest(()->b.get(genericEntity));
	}*/
}
