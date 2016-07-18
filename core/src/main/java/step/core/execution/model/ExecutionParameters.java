package step.core.execution.model;

import java.util.List;
import java.util.Map;

import step.core.artefacts.ArtefactFilter;
import step.core.repositories.RepositoryObjectReference;

public class ExecutionParameters {
	
	RepositoryObjectReference artefact;
	
	List<RepositoryObjectReference> exports;
	
	String description;
	
	String userID;
		
	ArtefactFilter artefactFilter;
	
	ExecutionMode mode;
	
	Map<String, String> customParameters;

	public ExecutionParameters() {
		super();
	}

	public ExecutionParameters(String userID, ArtefactFilter artefactFilter, ExecutionMode mode) {
		super();
		this.userID = userID;
		this.artefactFilter = artefactFilter;
		this.mode = mode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public ArtefactFilter getArtefactFilter() {
		return artefactFilter;
	}

	public void setArtefactFilter(ArtefactFilter artefactFilter) {
		this.artefactFilter = artefactFilter;
	}

	public ExecutionMode getMode() {
		return mode;
	}

	public void setMode(ExecutionMode mode) {
		this.mode = mode;
	}

	public RepositoryObjectReference getArtefact() {
		return artefact;
	}

	public void setArtefact(RepositoryObjectReference artefact) {
		this.artefact = artefact;
	}

	public List<RepositoryObjectReference> getExports() {
		return exports;
	}

	public void setExports(List<RepositoryObjectReference> exports) {
		this.exports = exports;
	}

	public Map<String, String> getCustomParameters() {
		return customParameters;
	}

	public void setCustomParameters(Map<String, String> customParameters) {
		this.customParameters = customParameters;
	}

}
