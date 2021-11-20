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
package step.client.planrunners;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import ch.exense.commons.io.Poller;
import step.client.AbstractRemoteClient;
import step.client.accessors.RemotePlanAccessor;
import step.client.collections.remote.RemoteCollectionFactory;
import step.client.credentials.ControllerCredentials;
import step.client.executions.RemoteExecutionManager;
import step.client.reports.RemoteReportTreeAccessor;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportTreeAccessor;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.runner.PlanRunner;
import step.core.plans.runner.PlanRunnerResult;
import step.core.repositories.RepositoryObjectReference;

/**
 * A runner that runs plans fully remotely
 * 
 * @deprecated This class will be removed in future releases. To run a plan
 *             located on your controller use {@link RemoteExecutionManager}
 *             instead.
 * 
 * @author Jérôme Comte
 *
 */
public class RemotePlanRunner extends AbstractRemoteClient implements PlanRunner {

	private PlanAccessor planAccessor;
	private RemoteCollectionFactory remoteCollectionFactory;

	public RemotePlanRunner() {
		super();
		remoteCollectionFactory = new RemoteCollectionFactory(this);
		planAccessor = new RemotePlanAccessor(remoteCollectionFactory);
	}

	public RemotePlanRunner(ControllerCredentials credentials) {
		super(credentials);
		remoteCollectionFactory = new RemoteCollectionFactory(this);
		planAccessor = new RemotePlanAccessor(remoteCollectionFactory);
	}

	@Override
	public PlanRunnerResult run(Plan plan) {
		return run(plan, new HashMap<>());
	}

	@Override
	public PlanRunnerResult run(Plan plan, Map<String, String> executionParameters) {
		planAccessor.save(plan);
		
		String planId = plan.getId().toString();
		String name = plan.getAttributes().get("name");
		
		return runPlanById(executionParameters, planId, name);
	}
	
	public PlanRunnerResult runPlanById(Map<String, String> executionParameters, String planId, String name) {
		ExecutionParameters params = new ExecutionParameters();
		HashMap<String, String> repositoryParameters = new HashMap<>();
		repositoryParameters.put(RepositoryObjectReference.PLAN_ID, planId);
		
		params.setRepositoryObject(new RepositoryObjectReference("local", repositoryParameters));
		params.setMode(ExecutionMode.RUN);
		params.setDescription(name);
		params.setUserID(credentials.getUsername());
		params.setCustomParameters(executionParameters);
		
		Builder b = requestBuilder("/rest/executions/start");
		Entity<ExecutionParameters> entity = Entity.entity(params, MediaType.APPLICATION_JSON);
		
		String executionId = executeRequest(()->b.post(entity, String.class));
		
		RemoteReportTreeAccessor reportTreeAccessor = new RemoteReportTreeAccessor(credentials);
		
		return new RemotePlanRunnerResult(executionId, executionId, reportTreeAccessor);
	}
	
	public class RemotePlanRunnerResult extends PlanRunnerResult {

		public RemotePlanRunnerResult(String executionId, String rootReportNodeId, ReportTreeAccessor reportTreeAccessor) {
			super(executionId, rootReportNodeId, reportTreeAccessor);
		}

		@Override
		public PlanRunnerResult waitForExecutionToTerminate(long timeout)
				throws TimeoutException, InterruptedException {
			RemotePlanRunner.this.waitForExecutionToTerminate(this.executionId, timeout);
			return this;
		}
		
	}
	
	public Execution getExecution(String executionID) {
		Builder b = requestBuilder("/rest/executions/"+executionID);
		return executeRequest(()->b.get(Execution.class));
	}
	
	public Execution waitForExecutionToTerminate(String executionID, long timeout) throws TimeoutException, InterruptedException {
		Poller.waitFor(()->getExecution(executionID).getStatus().equals(ExecutionStatus.ENDED), timeout);
		return getExecution(executionID);
	}
	
	public AbstractArtefact getArtefact(String artefactId) {
		Builder b = requestBuilder("/rest/controller/artefact/"+artefactId);
		return executeRequest(()->b.get(AbstractArtefact.class));
	}
}
