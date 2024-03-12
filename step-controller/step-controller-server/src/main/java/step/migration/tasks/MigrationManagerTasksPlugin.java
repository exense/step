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
package step.migration.tasks;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.repositories.RepositoryObjectManager;
import step.migration.MigrationManager;
import step.migration.MigrationManagerPlugin;

/**
 * This plugin is responsible for the registration of the migration tasks
 */
@Plugin(dependencies= {MigrationManagerPlugin.class})
public class MigrationManagerTasksPlugin extends AbstractControllerPlugin {

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		MigrationManager migrationManager = context.get(MigrationManager.class);
		migrationManager.addBinding(RepositoryObjectManager.class, context.getRepositoryObjectManager());
		migrationManager.register(SetSchedulerTaskAttributes.class);
		migrationManager.register(MigrateArtefactsToPlans.class);
		migrationManager.register(ScreenTemplateMigrationTask.class);
		migrationManager.register(RemoveLocalFunctions.class);
		migrationManager.register(ScreenTemplateArtefactTableMigrationTask.class);
		migrationManager.register(MigrateSeleniumFunctions.class);
		migrationManager.register(MigrateFunctionCallsById.class);
		migrationManager.register(ScreenInputHtmlTemplateMigrationTask.class);
		migrationManager.register(MigrateLogicFlowFunctions.class);
		migrationManager.register(ParameterPriorityScreenInputMigrationTask.class);
		migrationManager.register(ScreenEntityIconMigrationTask.class);
		migrationManager.register(FixPostgreSQLIndexes.class);
	}

	@Override
	public void migrateData(GlobalContext context) throws Exception {

	}

	@Override
	public void initializeData(GlobalContext context) throws Exception {

	}

	@Override
	public void afterInitializeData(GlobalContext context) throws Exception {

	}

	@Override
	public void serverStop(GlobalContext context) {

	}
}
