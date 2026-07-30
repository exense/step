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

import org.junit.Test;
import step.client.credentials.ControllerCredentials;
import step.client.credentials.SyspropCredendialsBuilder;
import step.client.executions.RemoteExecutionManager;
import step.client.reports.RemoteReportTreeAccessor;
import step.controller.multitenancy.Constants;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Verifies that the tenant (EE project name) carried by a {@link RemoteClientConfiguration} is applied
 * as the {@code Step-Project} HTTP header and inherited by nested clients such as the report tree
 * accessor used to fetch execution error summaries.
 * <p>
 * A token-less/username-less {@link ControllerCredentials} is used so that constructing the clients does
 * not trigger any network call (no login, no OAuth registration).
 */
public class RemoteClientTenantPropagationTest {

    private static final String URL = "http://localhost:8080";
    private static final String TENANT = "myTenant";

    private List<Object> tenantHeader(AbstractRemoteClient client) {
        return client.getHeaders().getHeaders(Constants.TENANT_HEADER);
    }

    @Test
    public void tenantHeaderIsSetInEeContext() {
        RemoteClientConfiguration config = new RemoteClientConfiguration(new ControllerCredentials(URL, null), TENANT);
        try (RemoteExecutionManager manager = new RemoteExecutionManager(config)) {
            assertEquals(List.of(TENANT), tenantHeader(manager));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void tenantHeaderIsInheritedByNestedReportTreeAccessor() {
        RemoteClientConfiguration config = new RemoteClientConfiguration(new ControllerCredentials(URL, null), TENANT);
        try (RemoteExecutionManager manager = new RemoteExecutionManager(config)) {
            // This is the client used by the error-summary path (getFuture(...).getErrorSummary()).
            RemoteReportTreeAccessor accessor = manager.getReportTreeAccessor();
            assertEquals(List.of(TENANT), tenantHeader(accessor));
            // The accessor is cached and shared, not recreated on each call.
            assertSame(accessor, manager.getReportTreeAccessor());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void noTenantHeaderInOsContext() {
        RemoteClientConfiguration config = new RemoteClientConfiguration(new ControllerCredentials(URL, null), null);
        try (RemoteExecutionManager manager = new RemoteExecutionManager(config)) {
            assertNull(tenantHeader(manager));
            assertNull(tenantHeader(manager.getReportTreeAccessor()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void legacyCredentialsConstructorRemainsTenantLess() {
        try (RemoteExecutionManager manager = new RemoteExecutionManager(new ControllerCredentials(URL, null))) {
            assertNull(tenantHeader(manager));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void tenantIsReadFromSystemProperty() {
        String prevUrl = System.getProperty("rcServerUrl");
        String prevToken = System.getProperty("rcToken");
        String prevTenant = System.getProperty(SyspropCredendialsBuilder.TENANT_PROPERTY);
        try {
            System.setProperty("rcServerUrl", URL);
            System.setProperty("rcToken", "someToken");
            System.setProperty(SyspropCredendialsBuilder.TENANT_PROPERTY, TENANT);

            RemoteClientConfiguration config = SyspropCredendialsBuilder.buildConfiguration();
            assertEquals(TENANT, config.getTenant());
            assertEquals(URL, config.getCredentials().getServerUrl());

            // Absent tenant property -> tenant-less configuration (OS context).
            System.clearProperty(SyspropCredendialsBuilder.TENANT_PROPERTY);
            assertNull(SyspropCredendialsBuilder.buildConfiguration().getTenant());
        } finally {
            restore("rcServerUrl", prevUrl);
            restore("rcToken", prevToken);
            restore(SyspropCredendialsBuilder.TENANT_PROPERTY, prevTenant);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
