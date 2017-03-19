package step.core.repositories;

import java.util.Map;

public interface Repository {

	public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception;

	public String importArtefact(Map<String, String> repositoryParameters) throws Exception;

	public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) throws Exception;

	public void exportExecution(Map<String, String> repositoryParameters, String executionID) throws Exception;

}