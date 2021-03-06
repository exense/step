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
package step.migration;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.repositories.RepositoryObjectManager;
import step.versionmanager.VersionManagerPlugin;

@Plugin(dependencies= {VersionManagerPlugin.class})
/**
 * This plugin is responsible for the registration of the MigrationManager
 */
public class MigrationManagerPlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		MigrationManager migrationManager = new MigrationManager();
		migrationManager.addBinding(RepositoryObjectManager.class, context.getRepositoryObjectManager());
		context.put(MigrationManager.class, migrationManager);
		
		super.executionControllerStart(context);
	}
	
}
