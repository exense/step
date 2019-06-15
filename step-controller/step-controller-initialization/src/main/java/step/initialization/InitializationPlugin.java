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
package step.initialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.access.User;
import step.core.access.UserAccessorImpl;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.versionmanager.VersionManager;
import step.versionmanager.VersionManagerPlugin;

@Plugin(dependencies= {VersionManagerPlugin.class})
public class InitializationPlugin extends AbstractPlugin {

	private static final Logger logger = LoggerFactory.getLogger(InitializationPlugin.class);
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		VersionManager versionManager = context.get(VersionManager.class);

		if(versionManager.getLatestControllerLog()==null) {
			// First start
			logger.info("Initializing Users...");
			setupUsers(context);
		}
						
		super.executionControllerStart(context);
	}

	private void setupUsers(GlobalContext context) {
		User user = new User();
		user.setUsername("admin");
		user.setRole("admin");
		user.setPassword(UserAccessorImpl.encryptPwd("init"));
		context.getUserAccessor().save(user);
	}

}
