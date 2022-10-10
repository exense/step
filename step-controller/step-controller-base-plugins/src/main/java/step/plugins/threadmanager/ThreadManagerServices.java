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
package step.plugins.threadmanager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.tags.Tag;
import step.artefacts.reports.TestCaseReportNode;
import step.common.managedoperations.Operation;
import step.common.managedoperations.OperationDetails;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.plugins.executiontypes.TestSetExecutionType;

@Path("/threadmanager")
@Tag(name = "System")
public class ThreadManagerServices extends AbstractStepServices {

	private ThreadManager threadManager;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		threadManager = getContext().get(ThreadManager.class);
	}

	@GET
	@Secured(right="operations-read")
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/operations/list")
	public List<OperationDetails> getCurrentOperationsList() {
		List<OperationDetails> operationListDetails = new ArrayList<OperationDetails>(); 
		for(ExecutionContext executionContext:getScheduler().getCurrentExecutions()) {
			String executionId = executionContext.getExecutionId();
			Plan plan = executionContext.getPlan();
			if(plan != null) {
				String planId = plan.getId().toString();
				String planName = plan.getAttributes().get(AbstractOrganizableObject.NAME);
				String executionType = executionContext.getExecutionType();
				// TODO implement this in a generic way
				if(TestSetExecutionType.NAME.equals(executionType)) {
					// in case of test set, get operations by test case
					Iterator<ReportNode> iterator = getContext().getReportAccessor().getReportNodesByExecutionIDAndClass(executionId, 
							TestCaseReportNode.class.getName());
					iterator.forEachRemaining(e->{
						String testcase = e.getName();
						threadManager.getCurrentOperationsByReportNodeId(e.getId().toString()).forEach(op->{
							operationListDetails.add(new OperationDetails(executionId, planId, planName, testcase, op));
						});
					});
				} else {
					threadManager.getCurrentOperations(executionContext).forEach(op->{
						operationListDetails.add(new OperationDetails(executionId, planId, planName, "", op));
					});
				}
			}
		}
		return operationListDetails;
	}	
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/operations")
	@Secured(right="execution-read")
	public List<Operation> getCurrentOperations(@QueryParam("eid") String executionID) {
		ExecutionContext executionContext = getExecutionRunnable(executionID);
		if(executionContext!=null) {
			return threadManager.getCurrentOperations(executionContext);
		} else {
			return new ArrayList<Operation>();
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/operations/{reportnodeid}")
	@Secured(right="execution-read")
	public List<Operation> getOperationsByReportNodeId(@PathParam("reportnodeid") String reportNodeId) {
		return threadManager.getCurrentOperationsByReportNodeId(reportNodeId);
	}
}
