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
import step.core.access.User;
import step.core.access.UserAccessor;
import step.framework.server.Session;


@Provider
@Priority(Priorities.AUTHENTICATION)//should be called before the framework one to set default user if required
public class NoSecurityFilter extends AbstractStepServices implements ContainerRequestFilter {

    private UserAccessor userAccessor;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        userAccessor = context.getUserAccessor();

    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Retrieve or initialize session
        retrieveOrInitializeSession();
    }

    protected Session retrieveOrInitializeSession() {
        Session session = getSession();
        if(session == null) {
            session = new Session();
            User anonymous = userAccessor.getByUsername("anonymous");
            if (anonymous == null) {
                anonymous = new User();
                anonymous.setUsername("anonymous");
                anonymous.setRole("admin");
                userAccessor.save(anonymous);
            }
            session.setUser(anonymous);
            session.setAuthenticated(false); //no authentication here, only setting
            setSession(session);
        }
        return session;
    }
}
