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
package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import step.controller.multitenancy.Tenant;
import step.controller.multitenancy.client.MultitenancyClient;

import java.util.List;
import java.util.Objects;

public abstract class TenantSwitcher {

	public Tenant switchTenant(String projectName, Log log) throws MojoExecutionException {
		try {
			Tenant currentTenant = switchTenant(projectName);
			log.info("Current tenant: " + currentTenant.getName() + " (" + currentTenant.getProjectId() + "). Is global: " + currentTenant.isGlobal());
			return currentTenant;
		} catch (MojoExecutionException e) {
			log.error("Unable to switch tenant");
			throw logAndThrow(log, e.getMessage(), e);
		}
	}

	protected Tenant switchTenant(String projectName) throws MojoExecutionException {
		try (MultitenancyClient multitenancyClient = createClient()) {
			List<Tenant> availableTenants = multitenancyClient.getAvailableTenants();

			Tenant currentTenant = null;
			if (availableTenants != null) {
				currentTenant = availableTenants.stream().filter(t -> Objects.equals(t.getName(), projectName)).findFirst().orElse(null);
			}
			if (currentTenant == null) {
				throw new MojoExecutionException("Unable to resolve tenant by name: " + projectName);
			}
			multitenancyClient.selectTenant(projectName);
			return currentTenant;
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	protected abstract MultitenancyClient createClient();

	protected MojoExecutionException logAndThrow(Log log, String errorText, Throwable e) {
		log.error(errorText, e);
		return new MojoExecutionException(errorText, e);
	}
}
