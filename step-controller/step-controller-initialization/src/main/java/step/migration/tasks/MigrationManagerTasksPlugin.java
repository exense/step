/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.migration.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.migration.MigrationManager;
import step.migration.MigrationManagerPlugin;

/**
 * This plugin is responsible for the registration of the migration tasks
 */
@Plugin(dependencies= {MigrationManagerPlugin.class})
public class MigrationManagerTasksPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(MigrationManagerTasksPlugin.class);
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		MigrationManager migrationManager = context.get(MigrationManager.class);
		migrationManager.register(new RenameArtefactType());
		migrationManager.register(new MigrateFunctions());
		migrationManager.register(new ScreenTemplateMigrationTask());
		migrationManager.register(new SetSchedulerTaskAttributes());
		migrationManager.register(new RemoveLocalFunctions());
	}
}
