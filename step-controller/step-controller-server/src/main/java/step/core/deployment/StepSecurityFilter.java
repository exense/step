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
package step.core.deployment;

import java.io.IOException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import step.core.GlobalContext;
import step.core.access.AuthenticationManager;
import step.framework.server.Session;

@Provider
@Priority(Priorities.AUTHENTICATION-1)//should be called before the framework one to set default user if required
public class StepSecurityFilter extends AbstractStepServices implements ContainerRequestFilter {

	private AuthenticationManager authenticationManager;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		authenticationManager = context.get(AuthenticationManager.class);
	}
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// Retrieve or initialize session
		Session session = retrieveOrInitializeSession();
		authenticationManager.authenticateDefaultUserIfAuthenticationIsDisabled(session);
	}
	
	protected Session retrieveOrInitializeSession() {
		Session session = getSession();
		if(session == null) {
			session = new Session();
			setSession(session);
		}
		return session;
	}
}
