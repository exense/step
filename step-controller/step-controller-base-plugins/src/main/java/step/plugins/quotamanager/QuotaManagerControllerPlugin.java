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
package step.plugins.quotamanager;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.io.FileWatchService;
import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.ExecutionEnginePlugin;

@Plugin
public class QuotaManagerControllerPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(QuotaManagerControllerPlugin.class);

	private boolean active = false;
	private FileWatchService fileWatchService;
	private QuotaManager quotaManager;

	@Override
	public void executionControllerStart(GlobalContext context) {
		context.getServiceRegistrationCallback().registerService(QuotaManagerServices.class);

		String config = context.getConfiguration().getProperty("quotamanager.config");
		if (config != null) {
			active = true;
			QuotaManager manager = initQuotaManager(config);
			context.put(QuotaManager.class, manager);
		}
	}

	@Override
	public ExecutionEnginePlugin getExecutionEnginePlugin() {
		if(active) {
			return new QuotaManagerPlugin(quotaManager);
		} else {
			return null;
		}
	}

	private QuotaManager initQuotaManager(String config) {
		final File configFile = new File(config);
		quotaManager = new QuotaManager(configFile);

		fileWatchService = new FileWatchService();
		fileWatchService.register(configFile, new Runnable() {
			@Override
			public void run() {
				quotaManager.loadConfiguration(configFile);
			}
		});
		return quotaManager;
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {
		if (fileWatchService != null) {
			try {
				fileWatchService.close();
			} catch (IOException e) {
				logger.error("Error while closing file watch service", e);
			}
		}
		super.executionControllerDestroy(context);
	}
}
