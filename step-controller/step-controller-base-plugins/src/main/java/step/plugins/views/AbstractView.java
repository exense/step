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
package step.plugins.views;

import java.util.concurrent.ConcurrentHashMap;

import step.core.artefacts.reports.ReportNode;


public abstract class AbstractView<V extends ViewModel> {

	private ConcurrentHashMap<String, V> models = new ConcurrentHashMap<>();
	
	public abstract V init();
	
	public abstract String getViewId();
	
	public V getModel(String executionId) {
		return models.get(executionId);
	}
	
	public V removeModel(String executionId) {
		return models.remove(executionId);
	}
	
	public void addModel(String executionId, V model) {
		models.put(executionId, model);
	}
	
	public abstract void afterReportNodeSkeletonCreation(V model, ReportNode node);
	
	public abstract void afterReportNodeExecution(V model, ReportNode node);
	
	public abstract void rollbackReportNode(V model, ReportNode node);
}
