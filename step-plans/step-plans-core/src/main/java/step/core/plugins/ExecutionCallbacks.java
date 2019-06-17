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
package step.core.plugins;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public interface ExecutionCallbacks extends PluginCallbacks {
	
	public void onLocalContextCreation(ExecutionContext context);
	
	public void afterReportNodeSkeletonCreation(ExecutionContext context, ReportNode node);
	
	public void beforeReportNodeExecution(ExecutionContext context, ReportNode node);
	
	public void afterReportNodeExecution(ExecutionContext context, ReportNode node);
	
	public void associateThread(ExecutionContext context, Thread thread);
	
	public void unassociateThread(ExecutionContext context, Thread thread);
	
	public void executionStart(ExecutionContext context);

	public void beforeExecutionEnd(ExecutionContext context);
	
	public void afterExecutionEnd(ExecutionContext context);
}
