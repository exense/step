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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import step.common.managedoperations.OperationManager;
import step.commons.conf.Configuration;
import step.commons.conf.FileWatchService;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class QuotaManagerPlugin extends AbstractPlugin {
		
	private ConcurrentHashMap<String, UUID> permits = new ConcurrentHashMap<>();

	private OperationManager operationManager;
		
	private QuotaManager quotaManager;
	
	@Override
	public void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
		Map<String, Object> bindings = new HashMap<>();
		
		bindings.putAll(context.getVariablesManager().getAllVariables());
		bindings.put("node", node);
		
		operationManager.enter("Waiting for quota", new Object());
		UUID permit;
		try {
			permit = quotaManager.acquirePermit(bindings);
		} catch (Exception e) {
			throw new RuntimeException("Error while getting permit from quota manager", e);
		} finally {
			operationManager.exit();
		}
		permits.put(node.getId().toString(), permit);
	}

	@Override
	public void afterReportNodeExecution(ReportNode node) {
		UUID permit = permits.remove(node.getId().toString());
		if(permit!=null) {
			quotaManager.releasePermit(permit);
		}
	}

	private QuotaManager initQuotaManager(String config) {		
		final File configFile = new File(config);
		quotaManager = new QuotaManager(configFile);
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
			context.put(QuotaManager.class, manager);
			context.getServiceRegistrationCallback().registerService(QuotaManagerServices.class);
		}
	}
}
