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
import step.framework.server.security.Secured;

/**
 * Dummy authentication filter for Step OS, only applies to Secured services. The filter authenticate as
 * the anonymous user if the session is not yet authenticated.
 * Make sure to use a lower priority filter (<1000) to bypass it and actually check authentication.
 * Such filter must abort the chain when not authenticated, the NoAuthenticationFilter does nothing in this context.
 */
@Provider
@Secured
@Priority(Priorities.AUTHENTICATION)
public class NoAuthenticationFilter extends AbstractStepServices implements ContainerRequestFilter {

    public static final String ANONYMOUS = "anonymous";
    private UserAccessor userAccessor;
    private User anonymousUser;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        userAccessor = context.getUserAccessor();
        anonymousUser = userAccessor.getByUsername(ANONYMOUS);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        authenticateAsAnonymousIfNeeded();
    }

    protected void authenticateAsAnonymousIfNeeded() {
        Session session = getSession();
        if (!session.isAuthenticated() && session.getUser() == null) {
            createAnonymousUserIfRequired();
            session.setUser(anonymousUser);
            session.setAuthenticated(true);
            setSession(session);
        }
    }

    private synchronized void createAnonymousUserIfRequired() {
        if (anonymousUser == null) {
            anonymousUser = new User();
            anonymousUser.setUsername(ANONYMOUS);
            anonymousUser.setRole("admin");
            userAccessor.save(anonymousUser);
        }
    }
}
