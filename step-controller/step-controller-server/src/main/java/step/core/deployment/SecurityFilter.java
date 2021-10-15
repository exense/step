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
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ExtendedUriInfo;

import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.RuntimeResource;
import step.core.GlobalContext;
import step.core.access.AccessManager;
import step.core.access.AuthenticationManager;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class SecurityFilter extends AbstractServices implements ContainerRequestFilter {
	
	@Inject
	private ExtendedUriInfo extendendUriInfo;
	
	
	
	private AuthenticationManager authenticationManager;
	private AccessManager accessManager;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		accessManager = context.get(AccessManager.class);
		authenticationManager = context.get(AuthenticationManager.class);
	}
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// Retrieve or initialize session
		Session session = retrieveOrInitializeSession();

		authenticationManager.authenticateDefaultUserIfAuthenticationIsDisabled(session);
		
		// Check rights
		Invocable invocable = extendendUriInfo.getMatchedResourceMethod().getInvocable();
		Secured classAnnotation = invocable.getHandler().getHandlerClass().getAnnotation(Secured.class);
		Secured annotation = invocable.getHandlingMethod().getAnnotation(Secured.class);
		if(annotation != null) {
			if(session.isAuthenticated()) {
				String right = annotation.right();
				if(right.length()>0) {
					if (classAnnotation != null && classAnnotation.right().length()>0) {
						right = classAnnotation.right() + right;
					}
					boolean hasRight = accessManager.checkRightInContext(session, right);
					if(!hasRight) {
						requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
					}
				}
			} else {
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			}
		}
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
