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
package step.artefacts.handlers;

import step.grid.TokenWrapperOwner;

public class CallFunctionTokenWrapperOwner implements TokenWrapperOwner {

	private String reportNodeId;
	private String executionId;
	private String executionDescription;
	
	public CallFunctionTokenWrapperOwner() {
		super();
	}

	public CallFunctionTokenWrapperOwner(String reportNodeId, String executionId, String executionDescription) {
		super();
		this.reportNodeId = reportNodeId;
		this.executionId = executionId;
		this.executionDescription = executionDescription;
	}

	public String getReportNodeId() {
		return reportNodeId;
	}

	public String getExecutionId() {
		return executionId;
	}

	public String getExecutionDescription() {
		return executionDescription;
	}

	public void setReportNodeId(String reportNodeId) {
		this.reportNodeId = reportNodeId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public void setExecutionDescription(String executionDescription) {
		this.executionDescription = executionDescription;
	}
}
