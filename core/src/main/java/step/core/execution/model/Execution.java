package step.core.execution.model;

import java.util.List;
import java.util.Map;

import org.jongo.marshall.jackson.oid.ObjectId;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;



public class Execution  {
	
	@ObjectId
	public String _id;
	
	Long startTime;
	
	Long endTime;
	
	String description;
			
	ExecutionStatus status;
	
	String artefactID;
	
	List<ReportExport> reportExports;
			
	String executionTaskID;
	
	@JsonSerialize(using = ExecutionParameterMapSerializer.class)
	@JsonDeserialize(using = ExecutionParameterMapDeserializer.class)
	Map<String, String> parameters;
	
	ExecutionParameters executionParameters;
		
	public Execution() {
		super();
	}

	public String getId() {
		return _id;
	}
	
	public void setId(String id) {
		_id = id;
	}
	
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ExecutionStatus getStatus() {
		return status;
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status;
	}

	public String getArtefactID() {
		return artefactID;
	}

	public void setArtefactID(String artefactID) {
		this.artefactID = artefactID;
	}

	public List<ReportExport> getReportExports() {
		return reportExports;
	}

	public void setReportExports(List<ReportExport> reportExports) {
		this.reportExports = reportExports;
	}

	public String getExecutionTaskID() {
		return executionTaskID;
	}

	public void setExecutionTaskID(String executionTaskID) {
		this.executionTaskID = executionTaskID;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public ExecutionParameters getExecutionParameters() {
		return executionParameters;
	}

	public void setExecutionParameters(ExecutionParameters executionParameters) {
		this.executionParameters = executionParameters;
	}

	@Override
	public String toString() {
		return "Execution [startTime=" + startTime + ", endTime=" + endTime + ", description=" + description 
				+ ", status=" + status + ", artefactID=" + artefactID + "]";
	}
}
