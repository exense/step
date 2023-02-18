package step.plugins.maven;

import java.util.Map;

public class RepositoryObjectReferenceDto {

	private String repositoryID;
	private Map<String, String> repositoryParameters;

	public RepositoryObjectReferenceDto() {
		super();
	}

	public RepositoryObjectReferenceDto(String repositoryID,
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

}
