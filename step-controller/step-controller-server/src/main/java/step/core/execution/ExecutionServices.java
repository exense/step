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
package step.core.execution;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.types.ObjectId;

import step.controller.services.async.AsyncTaskStatus;
import step.core.access.User;
import step.core.artefacts.reports.ReportNode;
import step.core.collections.SearchOrder;
import step.core.deployment.AbstractStepAsyncServices;
import step.core.deployment.ControllerServiceException;
import step.core.deployment.FindByCriteraParam;
import step.core.entities.EntityManager;
import step.core.execution.model.*;
import step.framework.server.Session;
import step.framework.server.security.Secured;
import step.core.repositories.RepositoryObjectReference;
import step.engine.execution.ExecutionLifecycleManager;
import step.framework.server.tables.service.TableService;
import step.framework.server.tables.service.bulk.TableBulkOperationReport;
import step.framework.server.tables.service.bulk.TableBulkOperationRequest;

@Singleton
@Path("executions")
@Tag(name = "Executions")
public class ExecutionServices extends AbstractStepAsyncServices {

	protected ExecutionAccessor executionAccessor;

	private TableService tableService;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		executionAccessor = getContext().getExecutionAccessor();
		tableService = getContext().require(TableService.class);
	}

	@Operation(description = "Starts an execution with the given parameters.")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/start")
	@Secured(right="plan-execute")
	public String execute(ExecutionParameters executionParams) {
		checkRightsOnBehalfOf("plan-execute", executionParams.getUserID());
		applyUserIdFromSession(executionParams);
		return getScheduler().execute(executionParams);
	}

	private void applyUserIdFromSession(ExecutionParameters executionParams) {
		// explicitly defined user id has a priority
		if (executionParams.getUserID() == null) {
			Session<User> session = getSession();
			if (session != null) {
				User user = session.getUser();
				if (user != null) {
					executionParams.setUserID(user.getUsername());
				}
			}
		}
	}

	@Operation(description = "Returns all executions.")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public List<Execution> getAll(@QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
		if(skip != null && limit != null) {
			return executionAccessor.getRange(skip, limit);
		} else {
			return getAll(0, 1000);
		}
	}

	@Operation(description = "Returns the execution with the given execution id.")
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public Execution getExecutionById(@PathParam("id") String id) {
		return executionAccessor.get(id);
	}

	@Operation(description = "Stops the execution with the given execution id.")
	@GET
	@Path("/{id}/stop")
	@Secured(right="plan-execute")
	public Void abort(@PathParam("id") String executionID) {
		ExecutionContext context = getExecutionRunnable(executionID);
		if(context!=null) {
			new ExecutionLifecycleManager(context).abort();
		}
		return null;
	}

	@Operation(description = "Force stop the execution with the given execution id.")
	@GET
	@Path("/{id}/force-stop")
	@Secured(right = "plan-execute")
	public Void forceStop(@PathParam("id") String executionID) {
		ExecutionContext context = getExecutionRunnable(executionID);
		if (context != null) {
			new ExecutionLifecycleManager(context).forceAbort();
		}
		return null;
	}

	@Operation(description = "Returns the executions matching the provided attributes.")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/search")
	@Secured(right="execution-read")
	public Execution getExecutionByAttribute(Map<String,String> attributes) {
		return executionAccessor.findByAttributes(attributes);
	}
    
	@Operation(description = "Returns a list of executions by the provided ids.")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/search/by/ids")
	@Secured(right="execution-read")
	public List<Execution> getExecutionsByIds(List<String> ids) {
		return executionAccessor.findByIds(ids).collect(Collectors.toList());
	}

	@Operation(description = "Returns the execution matching the provided repository object reference.")
	@POST
	@Path("/search/by/ref")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public List<Execution> getExecutionsByRepositoryObjectReference(RepositoryObjectReference objectReference) {
		return getContext().getExecutionAccessor().getTestExecutionsByArtefactURL(objectReference);
	}

	@Operation(description = "Returns the execution matching the given criteria.")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/search/by/critera")
	@Secured(right="execution-read")
	public List<Execution> findByCritera(FindByCriteraParam param) {
		return ((ExecutionAccessorImpl) getContext().getExecutionAccessor()).findByCritera(param.getCriteria(), 
				param.getStart().getTime(), param.getEnd().getTime(), new SearchOrder("endTime", -1),
				param.getSkip(), param.getLimit());
	}

	@Operation(description = "Returns the report nodes of the given execution and matching the given class.")
	@GET
	@Path("/{id}/reportnodes")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public List<ReportNode> getReportNodesByExecutionID(@PathParam("id") String executionID, @QueryParam("class") String reportNodeClass, @QueryParam("limit") int limit) {
		List<ReportNode> result = new ArrayList<>();
		Iterator<ReportNode> iterator;
		if(reportNodeClass!=null) {
			try (Stream<ReportNode> reportNodesByExecutionID = getContext().getReportAccessor().getReportNodesByExecutionIDAndClass(executionID, reportNodeClass)) {
				result = reportNodesByExecutionID.limit(limit).collect(Collectors.toList());
			}
		} else {
			try (Stream<ReportNode> reportNodesByExecutionID = getContext().getReportAccessor().getReportNodesByExecutionID(executionID)) {
				result = reportNodesByExecutionID.limit(limit).collect(Collectors.toList());
			}
		}
		return result;
	}

	@Operation(description = "Updates the provided execution.")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="execution-write")
	public Execution saveExecution(Execution execution) {
		return executionAccessor.save(execution);
	}

	@Operation(description = "Delete the execution with the given execution id, use the housekeeping services for full deletion")
	@DELETE
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="execution-delete")
	public void deleteExecution(@PathParam("id") String id) {
		Execution execution = executionAccessor.get(id);
		if (execution.getStatus().equals(ExecutionStatus.ENDED)) {
			throw new ControllerServiceException("Only ended executions can be deleted.");
		}
		executionAccessor.remove(new ObjectId(id));
	}

	@Operation(description = "Restart multiple executions according to the provided parameters.")
	@POST
	@Path("/bulk/restart")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-execute")
	public AsyncTaskStatus<TableBulkOperationReport> restartExecutions(TableBulkOperationRequest request) {
		Consumer<String> consumer = t -> {
			try {
				ExecutionParameters executionParameters = executionAccessor.get(t).getExecutionParameters();
				applyUserIdFromSession(executionParameters);
				execute(executionParameters);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		return scheduleAsyncTaskWithinSessionContext(h ->
				tableService.performBulkOperation(EntityManager.executions, request, consumer, getSession()));
	}

	@Operation(description = "Stop multiple executions according to the provided parameters.")
	@POST
	@Path("/bulk/stop")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-execute")
	public AsyncTaskStatus<TableBulkOperationReport> stopExecutions(TableBulkOperationRequest request) {
		Consumer<String> consumer = t -> {
			try {
				abort(t);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		return scheduleAsyncTaskWithinSessionContext(h ->
				tableService.performBulkOperation(EntityManager.executions, request, consumer, getSession()));
	}

}
