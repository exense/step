/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.plugins.threadmanager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import step.common.managedoperations.Operation;
import step.common.managedoperations.OperationDetails;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.execution.ExecutionRunnable;

@Path("/threadmanager")
public class ThreadManagerServices extends AbstractServices {

	@GET
	@Secured(right="admin")
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/operations/list")
	public List<OperationDetails> getCurrentOperationsList() {
		ThreadManager threadManager = (ThreadManager) getContext().get(ThreadManager.THREAD_MANAGER_INSTANCE_KEY);
		List<OperationDetails> operationListDetails = new ArrayList<OperationDetails>(); 
		for(ExecutionRunnable task:getScheduler().getCurrentExecutions()) {
			if(task!=null) {
				String executionId = task.getContext().getExecutionId();
				String planId = task.getContext().getPlan().getId().toString();
				String planName = task.getContext().getPlan().getAttributes().get("name");
				Iterator<ReportNode> iterator = getContext().getReportAccessor().getReportNodesByExecutionIDAndClass(executionId, 
						"step.artefacts.reports.TestCaseReportNode");
				//in case of test case, get operations by test case
				if (iterator.hasNext()) {
					iterator.forEachRemaining(e->{
						String testcase = e.getName();
						threadManager.getCurrentOperationsByReportNodeId(e.getId().toString()).forEach(op->{
							operationListDetails.add(new OperationDetails(executionId, planId, planName, testcase, op));
						});
					});
				}
				//else get operation by eid
				else {
					threadManager.getCurrentOperations(task.getContext()).forEach(op->{
						operationListDetails.add(new OperationDetails(executionId, planId, planName, "", op));
					});
				}
			}
		}
		return operationListDetails;
	}	
	
	@GET
	@Secured(right="report-read")
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/operations")
	public List<Operation> getCurrentOperations(@QueryParam("eid") String executionID) {
		ThreadManager threadManager = (ThreadManager) getContext().get(ThreadManager.THREAD_MANAGER_INSTANCE_KEY);

		ExecutionRunnable task = getExecutionRunnable(executionID);
		if(task!=null) {
			return threadManager.getCurrentOperations(task.getContext());
		} else {
			return new ArrayList<Operation>();
		}
	}
	
	@GET
	@Secured(right="report-read")
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/operations/{reportnodeid}")
	public List<Operation> getOperationsByReportNodeId(@PathParam("reportnodeid") String reportNodeId) {
		ThreadManager threadManager = (ThreadManager) getContext().get(ThreadManager.THREAD_MANAGER_INSTANCE_KEY);
		return threadManager.getCurrentOperationsByReportNodeId(reportNodeId);
	}
}
