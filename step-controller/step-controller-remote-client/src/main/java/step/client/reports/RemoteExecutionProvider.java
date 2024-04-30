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
package step.client.reports;

import jakarta.ws.rs.client.Invocation.Builder;
import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionProvider;

public class RemoteExecutionProvider extends AbstractRemoteClient implements ExecutionProvider {

	public RemoteExecutionProvider(ControllerCredentials credentials) {
		super(credentials);
	}

	public RemoteExecutionProvider(){
		super();
	}

	@Override
	public Execution get(String id) {
		Builder b = requestBuilder("/rest/executions/" + id);
		return executeRequest(() -> b.get(Execution.class));
	}

}
