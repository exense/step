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
package step.client.executions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;

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
	 * @param planId the ID of the plan to be executed.
	 * @return the execution ID of the execution
	 */
	public String execute(String planId) {
		return execute(planId, new HashMap<>());
	}
	
	/**
	 * Executes a plan located on the controller
	 * 
	 * @param planId the ID of the plan to be executed.
	 * @param executionParameters the execution parameters (the drop-downs that are set on the execution screen in the UI)
	 * @return the execution ID of the execution
	 */
	public String execute(String planId, Map<String, String> executionParameters) {
		Map<String, String> repositoryParameters = new HashMap<>();
		repositoryParameters.put(RepositoryObjectReference.PLAN_ID, planId);
		return executeFromExternalRepository(RepositoryObjectReference.LOCAL_REPOSITORY_ID, repositoryParameters, executionParameters);
	}

	/**
	 * Executes a plan located on an external repository
	 * 
	 * @param repositoryId the ID of the repository the Plan is located on
	 * @param repositoryParameters the parameters to be passed to the repository to locate the plan 
	 * @return the execution ID of the execution
	 */
	public String executeFromExternalRepository(String repositoryId, Map<String, String> repositoryParameters) {
		return executeFromExternalRepository(repositoryId, repositoryParameters, new HashMap<>());
	}
	
	/**
	 * Executes a plan located on an external repository
	 * 
	 * @param repositoryId the ID of the repository the Plan is located on
	 * @param repositoryParameters the parameters to be passed to the repository to locate the plan 
	 * @param executionParameters the execution parameters (the drop-downs that are set on the execution screen in the UI)
	 * @return the execution ID of the execution
	 */
	public String executeFromExternalRepository(String repositoryId, Map<String, String> repositoryParameters, Map<String, String> executionParameters) {
		RepositoryObjectReference repositoryObjectReference = new RepositoryObjectReference(repositoryId, repositoryParameters);

		ExecutionParameters executionParameterObject = new ExecutionParameters();
		executionParameterObject.setRepositoryObject(repositoryObjectReference);
		executionParameterObject.setUserID(credentials.getUsername());
		executionParameterObject.setMode(ExecutionMode.RUN);
		executionParameterObject.setCustomParameters(executionParameters);
		
		return execute(executionParameterObject);
	}
	
	/**
	 * (Advanced) Executes a plan located on the controller using the provided {@link ExecutionParameters} object
	 * 
	 * @param executionParameterObject the {@link ExecutionParameters} 
	 * @return the execution ID of the execution
	 */
	public String execute(ExecutionParameters executionParameterObject) {
		Builder b = requestBuilder("/rest/executions/start");
		Entity<?> entity = Entity.entity(executionParameterObject, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity, String.class));
	}
	
	/**
	 * Stop an execution
	 * 
	 * @param executionId the ID of the execution to be stopped 
	 */
	public void stop(String executionId) {
		Builder b = requestBuilder("/rest/executions/"+executionId+"/stop");
		executeRequest(()->b.get());
	}
	
	/**
	 * @param executionId the ID of the execution
	 * @return the {@link Execution}
	 */
	public Execution get(String executionId) {
		Builder b = requestBuilder("/rest/executions/"+executionId);
		return executeRequest(()->b.get(Execution.class));
	}

	/**
	 * @param executionId the ID of the execution
	 * @param reportNodeClass the classname of the {@link ReportNode} to be queried
	 * @return the distribution of {@link ReportNodeStatus} for the {@link ReportNode}s specified as argument
	 */
	@SuppressWarnings("unchecked")
	public Map<ReportNodeStatus, Integer> getStatusReport(String executionId, String reportNodeClass) {
		throw new RuntimeException("Not supported anymore since 3.17");

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
