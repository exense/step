/*
 * Copyright (C) 2025, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.core.deployment;

import step.core.access.User;
import step.framework.server.Session;

/**
 * When using this class as context for {@link step.core.objectenricher.ObjectHook} related operations,
 * the scope of the operations is restricted to non-global entities.
 * <p>
 * This class has been introduced to perform {@link step.framework.server.tables.service.TableService} requests
 * without global entities.
 * <p>
 * In the future we might introduce a request parameter object to perform such operations without having to
 * "misuse" the context
 */
public class RestrictedScopeSession extends Session<User> {

    public RestrictedScopeSession(Session<User> session) {
        session.getKeys().stream().forEach(k -> put(k, session.get(k)));
        setAuthenticated(session.isAuthenticated());
        setUser(session.getUser());
        setToken(session.getToken());
        setTokenType(session.getTokenType());
    }
}
