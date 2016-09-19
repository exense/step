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
package step.plugins.quotamanager;

import java.io.File;

import step.commons.conf.Configuration;
import step.commons.conf.FileWatchService;
import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class QuotaManagerPlugin extends AbstractPlugin {
	
	public static final String QUOTAMANAGER_KEY = "QuotaManager_Instance";
	
	private QuotaManager initQuotaManager(String config) {		
		final File configFile = new File(config);
		final QuotaManager quotaManager = new QuotaManager(configFile);
		FileWatchService.getInstance().register(configFile, new Runnable() {
			@Override
			public void run() {
				quotaManager.loadConfiguration(configFile);
			}
		});
		return quotaManager;
	}
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		String config = Configuration.getInstance().getProperty("quotamanager.config");
		if(config!=null) {
			QuotaManager manager = initQuotaManager(config);
			context.put(QUOTAMANAGER_KEY, manager);
			context.getServiceRegistrationCallback().registerService(QuotaManagerServices.class);
		}
	}
}
