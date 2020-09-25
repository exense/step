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
package step.client.controller;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;

public class ControllerServicesClient extends AbstractRemoteClient {

	public ControllerServicesClient() {
		super();
	}

	public ControllerServicesClient(ControllerCredentials credentials) {
		super(credentials);
	}

	public void shutdownController() {
		Builder r = requestBuilder("/rest/controller/shutdown");
		executeRequest(()->r.post(Entity.entity(null, MediaType.APPLICATION_JSON)));
	}
	
	public void changeMyPassword(String newPwd) {
		Builder r = requestBuilder("/rest/admin/myaccount/changepwd");
		executeRequest(()->r.post(Entity.entity("{\"newPwd\" : \""+newPwd+"\"}", MediaType.APPLICATION_JSON)));
	}
}
