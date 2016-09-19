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
package step.plugins.progressunit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class ProgressUnitPlugin extends AbstractPlugin {
	
	public static final String PROGRESS_UNIT_KEY = "ProgressUnitPlugin";
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		context.getServiceRegistrationCallback().registerService(ProgressUnitServices.class);
	}

	@Override
	public void executionStart(ExecutionContext context) {
		ProgressUnit unit = new ProgressUnit();
		unit.addProgressView("step.artefacts.handlers.teststep.TestStepProgressView");
		context.put(PROGRESS_UNIT_KEY, unit);
	}

	static Logger logger = LoggerFactory.getLogger(ProgressUnitPlugin.class); 
		
	private ProgressUnit getProgressUnit() {
		Object value = ExecutionContext.getCurrentContext().get(PROGRESS_UNIT_KEY);
		return value!=null?(ProgressUnit) value:null;
	}
	
	@Override
	public void afterReportNodeSkeletonCreation(ReportNode node) {
		getProgressUnit().afterReportNodeSkeletonCreation(node);
	}
	
	@Override
	public void afterReportNodeExecution(ReportNode node) {
		getProgressUnit().afterReportNodeExecution(node);
	}

}
