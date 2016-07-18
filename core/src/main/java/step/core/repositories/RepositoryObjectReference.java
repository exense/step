package step.core.repositories;

import java.util.Map;

public class RepositoryObjectReference {

	String repositoryID;
	
	Map<String, String> repositoryParameters;

	public RepositoryObjectReference() {
		super();
	}

	public RepositoryObjectReference(String repositoryID,
			Map<String, String> repositoryParameters) {
		super();
		this.repositoryID = repositoryID;
		this.repositoryParameters = repositoryParameters;
	}

	public String getRepositoryID() {
		return repositoryID;
	}

	public void setRepositoryID(String repositoryID) {
		this.repositoryID = repositoryID;
	}

	public Map<String, String> getRepositoryParameters() {
		return repositoryParameters;
	}

	public void setRepositoryParameters(Map<String, String> repositoryParameters) {
		this.repositoryParameters = repositoryParameters;
	}

	@Override
	public String toString() {
		return "RepositoryObjectReference [repositoryID=" + repositoryID
				+ ", repositoryParameters=" + repositoryParameters + "]";
	}
}
