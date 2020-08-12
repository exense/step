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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin
@IgnoreDuringAutoDiscovery
public class QuotaManagerPlugin extends AbstractExecutionEnginePlugin {

	private final QuotaManager quotaManager;
	private final ConcurrentHashMap<String, UUID> permits = new ConcurrentHashMap<>();

	public QuotaManagerPlugin(QuotaManager quotaManager) {
		super();
		this.quotaManager = quotaManager;
	}

	@Override
	public void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
		Map<String, Object> bindings = new HashMap<>();

		bindings.putAll(context.getVariablesManager().getAllVariables());
		bindings.put("node", node);

		UUID permit;
		try {
			permit = quotaManager.acquirePermit(bindings);
		} catch (Exception e) {
			throw new PluginCriticalException("Error while getting permit from quota manager", e);
		} finally {
		}
		permits.put(node.getId().toString(), permit);
	}

	@Override
	public void afterReportNodeExecution(ReportNode node) {
		UUID permit = permits.remove(node.getId().toString());
		if (permit != null) {
			quotaManager.releasePermit(permit);
		}
	}
}
