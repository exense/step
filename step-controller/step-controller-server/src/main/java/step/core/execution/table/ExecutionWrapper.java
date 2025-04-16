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
package step.core.execution.table;

import step.core.artefacts.reports.ReportNode;
import step.core.execution.model.Execution;

public class ExecutionWrapper extends Execution {
	
	private ReportNode rootReportNode;
	
	private Object executionSummary;

	public ReportNode getRootReportNode() {
		return rootReportNode;
	}

	public void setRootReportNode(ReportNode rootReportNode) {
		this.rootReportNode = rootReportNode;
	}

	public Object getExecutionSummary() {
		return executionSummary;
	}

	public void setExecutionSummary(Object executionSummary) {
		this.executionSummary = executionSummary;
	}
}
