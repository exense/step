package step.client.executions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import ch.exense.commons.io.Poller;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.repositories.RepositoryObjectReference;

/**
 * This class provides an API for the execution of plans existing on a remote controller
 *
 */
public class RemoteExecutionManager extends AbstractRemoteClient {
	
	public RemoteExecutionManager() {
		super();
	}

	public RemoteExecutionManager(ControllerCredentials credentials) {
		super(credentials);
	}
	
	/**
	 * Executes a plan located on the controller
	 *  
	 * @param planId the ID of the plan to be executed. The plan should execute on the controller
	 * @return the execution ID of the execution
	 */
	public String execute(String planId) {
		return execute(planId, new HashMap<>());
	}
	
	/**
	 * Executes a plan located on the controller
	 * 
	 * @param planId the ID of the plan to be executed. The plan should execute on the controller
	 * @param parameters the execution parameters (the drop-downs that are set on the execution screen in the UI)
	 * @return the execution ID of the execution
	 */
	public String execute(String planId, Map<String, String> parameters) {
		Map<String, String> repositoryParameters = new HashMap<>();
		repositoryParameters.put(RepositoryObjectReference.PLAN_ID, planId);
		RepositoryObjectReference repositoryObjectReference = new RepositoryObjectReference("local", repositoryParameters);
		ExecutionParameters executionParameters = new ExecutionParameters();
		executionParameters.setRepositoryObject(repositoryObjectReference);
		executionParameters.setUserID(credentials.getUsername());
		executionParameters.setMode(ExecutionMode.RUN);
		executionParameters.setIsolatedExecution(false);
		executionParameters.setCustomParameters(parameters);
		return execute(executionParameters);
	}

	/**
	 * Executes a plan located on an external repository
	 * 
	 * @param repositoryId the ID of the repository the Plan is located on
	 * @param repositoryParameters the parameters to be passed to the repository to locate the plan 
	 * @return the execution ID of the execution
	 */
	public String executeFromExternalRepository(String repositoryId, Map<String, String> repositoryParameters) {
		RepositoryObjectReference repositoryObjectReference = new RepositoryObjectReference(repositoryId, repositoryParameters);
		ExecutionParameters executionParameters = new ExecutionParameters();
		executionParameters.setRepositoryObject(repositoryObjectReference);
		executionParameters.setUserID(credentials.getUsername());
		executionParameters.setMode(ExecutionMode.RUN);
		executionParameters.setIsolatedExecution(false);
		return execute(executionParameters);
	}
	
	public String execute(ExecutionParameters executionParams) {
		Builder b = requestBuilder("/rest/controller/execution/");
		Entity<?> entity = Entity.entity(executionParams, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity, String.class));
	}
	
	/**
	 * Stop an execution
	 * 
	 * @param executionId the ID of the execution to be stopped 
	 */
	public void stop(String executionId) {
		Builder b = requestBuilder("/rest/controller/execution/"+executionId+"/stop");
		executeRequest(()->b.get());
	}
	
	/**
	 * @param executionId the ID of the execution
	 * @return the {@link Execution}
	 */
	public Execution get(String executionId) {
		Builder b = requestBuilder("/rest/controller/execution/"+executionId);
		return executeRequest(()->b.get(Execution.class));
	}

	/**
	 * @param executionId the ID of the execution
	 * @param reportNodeClass the classname of the {@link ReportNode} to be queried
	 * @return the distribution of {@link ReportNodeStatus} for the {@link ReportNode}s specified as argument
	 */
	@SuppressWarnings("unchecked")
	public Map<ReportNodeStatus, Integer> getStatusReport(String executionId, String reportNodeClass) {
		Map<String, String> params = new HashMap<>();
		if(reportNodeClass != null) {
			params.put("class", reportNodeClass);
		}
		Builder b = requestBuilder("/rest/controller/execution/"+executionId+"/statusdistribution", params);
		return executeRequest(()->b.get(Map.class));
	}
	
	/**
	 * Waits for an execution to terminate
	 * 
	 * @param executionID 
	 * @param timeout the timeout in ms
	 * @return 
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	public Execution waitForTermination(String executionID, long timeout) throws TimeoutException, InterruptedException {
		Poller.waitFor(()->get(executionID).getStatus().equals(ExecutionStatus.ENDED), timeout);
		return get(executionID);
	}
	
	/**
	 * Returns a future representing of the execution
	 * 
	 * @param executionId
	 * @return
	 */
	public RemoteExecutionFuture getFuture(String executionId) {
		return new RemoteExecutionFuture(this, executionId);
	}
	
	protected ControllerCredentials getControllerCredentials() {
		return credentials;
	}

}
