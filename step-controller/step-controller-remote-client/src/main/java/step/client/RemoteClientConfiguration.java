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
package step.client;

import step.client.credentials.ControllerCredentials;

import java.util.Objects;

/**
 * Bundles everything required to establish a remote connection to a Step controller:
 * the {@link ControllerCredentials} and, in EE context, the tenant (project name) the
 * requests must be scoped to.
 * <p>
 * Passing this object to {@link AbstractRemoteClient} (rather than bare credentials) lets
 * the client set the tenant HTTP header itself and lets nested clients inherit it, avoiding
 * the manual and error-prone header propagation that was previously scattered across callers.
 * <p>
 * The tenant is optional: it is {@code null} in OS context (no multitenancy), in which case
 * no tenant header is sent.
 */
public class RemoteClientConfiguration {

    private final ControllerCredentials credentials;

    /**
     * EE project name to scope requests to; {@code null} in OS context.
     */
    private final String tenant;

    public RemoteClientConfiguration(ControllerCredentials credentials) {
        this(credentials, null);
    }

    public RemoteClientConfiguration(ControllerCredentials credentials, String tenant) {
        this.credentials = Objects.requireNonNull(credentials, "credentials must not be null");
        this.tenant = tenant;
    }

    public ControllerCredentials getCredentials() {
        return credentials;
    }

    public String getTenant() {
        return tenant;
    }

    /**
     * @return a copy of this configuration scoped to the given tenant.
     */
    public RemoteClientConfiguration withTenant(String tenant) {
        return new RemoteClientConfiguration(credentials, tenant);
    }
}
