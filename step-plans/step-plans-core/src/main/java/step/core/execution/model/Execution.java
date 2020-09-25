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
package step.core.execution.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.plans.Plan;
import step.core.repositories.ImportResult;



public class Execution extends AbstractOrganizableObject {
	
	Long startTime;
	
	Long endTime;
	
	String description;
	
	String executionType;
			
	ExecutionStatus status;
	
	ReportNodeStatus result;
	
	String planId;
	
	ImportResult importResult;
	
	List<ReportExport> reportExports;
			
	String executionTaskID;
	
	@JsonSerialize(using = ExecutionParameterMapSerializer.class)
	@JsonDeserialize(using = ExecutionParameterMapDeserializer.class)
	Map<String, String> parameters;
	
	ExecutionParameters executionParameters;
		
	public Execution() {
		super();
	}

	/**
	 * @return the start time of the execution in epoch format
	 */
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	/**
	 * @return the end time of the execution in epoch format
	 */
	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	/**
	 * @return the plain text description of the description as displayed 
	 * in the column "Description" of the execution table 
	 */
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the result (overall status) of the execution as displayed in
	 * the column "Result" of the execution table
	 */
	public ReportNodeStatus getResult() {
		return result;
	}

	public void setResult(ReportNodeStatus result) {
		this.result = result;
	}

	/**
	 * @return the current status of the execution as displayed in the column
	 * "Status" of the execution tbale
	 */
	public ExecutionStatus getStatus() {
		return status;
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status;
	}

	/**
	 * @return the ID of the executed {@link Plan}
	 */
	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	/**
	 * @return the result of the import phase from the external repository (ALM, Jira, etc)
	 */
	public ImportResult getImportResult() {
		return importResult;
	}

	public void setImportResult(ImportResult importResult) {
		this.importResult = importResult;
	}

	/**
	 * @return the result of export of the results to the external repository (ALM, Jira, etc)
	 */
	public List<ReportExport> getReportExports() {
		return reportExports;
	}

	public void setReportExports(List<ReportExport> reportExports) {
		this.reportExports = reportExports;
	}

	/**
	 * @return the ID of the scheduler task (if any) this execution has been triggered from 
	 */
	public String getExecutionTaskID() {
		return executionTaskID;
	}

	public void setExecutionTaskID(String executionTaskID) {
		this.executionTaskID = executionTaskID;
	}

	/**
	 * @return the type of execution ("Default", "TestSet")
	 */
	public String getExecutionType() {
		return executionType;
	}

	public void setExecutionType(String executionType) {
		this.executionType = executionType;
	}

	/**
	 * @return the list of parameters used by this execution as displayed in the tab "Execution parameters" of the 
	 * Execution view
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	/**
	 * @return the {@link ExecutionParameters} used for this execution
	 */
	public ExecutionParameters getExecutionParameters() {
		return executionParameters;
	}

	public void setExecutionParameters(ExecutionParameters executionParameters) {
		this.executionParameters = executionParameters;
	}

	@Override
	public String toString() {
		return "Execution [startTime=" + startTime + ", endTime=" + endTime + ", description=" + description 
				+ ", status=" + status + ", planID=" + planId + "]";
	}
}
