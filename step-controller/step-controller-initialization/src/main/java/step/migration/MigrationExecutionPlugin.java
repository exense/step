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
package step.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.Version;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.migration.tasks.MigrationManagerTasksPlugin;
import step.versionmanager.ControllerLog;
import step.versionmanager.VersionManager;

@Plugin(dependencies= {MigrationManagerTasksPlugin.class})
/**
 * This plugin is responsible for the execution of the Migration Tasks
 */
public class MigrationExecutionPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(MigrationExecutionPlugin.class);
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		checkVersion(context);	
				
		super.executionControllerStart(context);
	}

	private void checkVersion(GlobalContext context) {
		MigrationManager migrationManager = context.get(MigrationManager.class);
		VersionManager versionManager = context.get(VersionManager.class);
		
		ControllerLog latestLog = versionManager.getLatestControllerLog();
		if(latestLog!=null) {
			Version latestVersion = latestLog.getVersion();
			// Version tracking has been introduced with 3.8.0 therefore assuming version 3.7.0 as latest version if null
			if(latestVersion==null) {
				latestVersion = new Version(3, 7, 0);
			}
			if(context.getCurrentVersion().compareTo(latestVersion)>0) {
				logger.info("Starting controller with a newer version. Current version is "
						+context.getCurrentVersion()+". Version of last start was "+latestVersion);
			} else if (context.getCurrentVersion().compareTo(latestVersion)<0) {
				logger.info("Starting controller with an older version. Current version is "
						+context.getCurrentVersion()+". Version of last start was "+latestVersion);
			}
			migrationManager.migrate(latestVersion, context.getCurrentVersion());
		}					
	}
}
