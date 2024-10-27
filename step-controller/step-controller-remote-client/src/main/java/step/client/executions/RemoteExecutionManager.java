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

import ch.exense.commons.io.Poller;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import step.artefacts.reports.CustomReportType;
import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.repositories.RepositoryObjectReference;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeoutException;

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
	 * @return the content of the report
	 */
	public Report getCustomReport(String executionId, String reportType){
		Builder b = requestBuilder("/rest/executions/" + executionId + "/report/" + reportType);
		return executeRequest(() -> {
			Response response = b.get();
			try(InputStream contentIs = response.readEntity(InputStream.class)) {
				return new Report(getFileNameFromContentDisposition(response, executionId), contentIs.readAllBytes());
			} catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
	}

	private String getFileNameFromContentDisposition(Response response, String executionId) {
		String res = null;
		String header = response.getHeaderString("content-disposition");
		if (header != null) {
			String[] split = header.split("filename=");
			if (split.length > 1) {
				String[] split2 = split[1].split(";");
				if (split2.length > 0) {
					res = split2[0];
					if (res != null) {
						res = res.replace("\"", "");
					}
				}
			}
		}
		String formattedTimestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSSSS").format(LocalDateTime.now()) ;
		return res == null ? formattedTimestamp + "-" + CustomReportType.JUNIT.name().toLowerCase() + ".xml" : res;
	}

	public static class Report {
		private String fileName;
		private byte[] content;

		public Report(String fileName, byte[] content) {
			this.fileName = fileName;
			this.content = content;
		}

		public String getFileName() {
			return fileName;
		}

		public byte[] getContent() {
			return content;
		}
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

	public List<Execution> waitForTermination(List<String> executionIds, long timeout) throws TimeoutException, InterruptedException {
		Set<String> pendingExecutions = new HashSet<>(executionIds);
		Poller.waitFor(() -> {
			Set<String> completed = new HashSet<>();
			for (String e : pendingExecutions) {
				if (get(e).getStatus().equals(ExecutionStatus.ENDED)) {
					completed.add(e);
				}
			}
			pendingExecutions.removeAll(completed);
			return pendingExecutions.isEmpty();
		}, timeout);

		List<Execution> res = new ArrayList<>();
		for (String e : executionIds) {
			Execution executionObj = get(e);
			res.add(executionObj);
		}
		return res;
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
