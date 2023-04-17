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
package step.controller.multitenancy.client;

import java.util.List;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.controller.multitenancy.Tenant;

public class RemoteMultitenancyClientImpl extends AbstractRemoteClient implements MultitenancyClient {

	public RemoteMultitenancyClientImpl(ControllerCredentials credentials) {
		super(credentials);
	}

	@Override
	public void selectTenant(String tenantName) throws Exception {
		Builder b = requestBuilder("/rest/tenants/current");
		executeRequest(()->b.post(Entity.entity(tenantName, MediaType.APPLICATION_JSON)));
	}
	
	@Override
	public List<Tenant> getAvailableTenants() {
		Builder b = requestBuilder("/rest/tenants");
		return executeRequest(()->b.get(new GenericType<List<Tenant>>() {}));
	}
	
	@Override
	public Tenant getCurrentTenant() {
		Builder b = requestBuilder("/rest/tenants/current");
		return executeRequest(()->b.get(Tenant.class));
	}
}
