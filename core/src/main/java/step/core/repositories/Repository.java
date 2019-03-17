package step.core.repositories;

import java.util.Map;

import step.core.execution.ExecutionContext;

public interface Repository {

	public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception;

	public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) throws Exception;

	public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception;

	public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception;

}