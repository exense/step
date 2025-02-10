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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import step.reports.CustomReportType;
import step.controller.services.async.AsyncTaskStatus;
import step.core.access.User;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.artefacts.reports.aggregated.AggregatedReportViewBuilder;
import step.core.artefacts.reports.junitxml.JUnitXmlReportBuilder;
import step.core.collections.SearchOrder;
import step.core.deployment.AbstractStepAsyncServices;
import step.core.deployment.ControllerServiceException;
import step.core.deployment.FindByCriteraParam;
import step.core.entities.EntityManager;
import step.core.execution.model.*;
import step.core.repositories.RepositoryObjectReference;
import step.framework.server.Session;
import step.framework.server.security.Secured;
import step.framework.server.tables.service.TableService;
import step.framework.server.tables.service.bulk.TableBulkOperationReport;
import step.framework.server.tables.service.bulk.TableBulkOperationRequest;
import step.reporting.JUnitReport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	@Secured(right = "plan-execute")
	public String execute(ExecutionParameters executionParams) {
		checkRightsOnBehalfOf("plan-execute", executionParams.getUserID());
		applyUserIdFromSessionIfNotSpecified(executionParams);
		return getScheduler().execute(executionParams);
	}

	private void applyUserIdFromSessionIfNotSpecified(ExecutionParameters executionParams) {
		// explicitly defined user id has a priority
		if (executionParams.getUserID() == null) {
			applyUserIdFromSession(executionParams);
		}
	}

	private void applyUserIdFromSession(ExecutionParameters executionParams) {
		Session<User> session = getSession();
		if (session != null) {
			User user = session.getUser();
			if (user != null) {
				executionParams.setUserID(user.getUsername());
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
			ExecutionEngineRunner.abort(context);
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
			ExecutionEngineRunner.forceAbort(context);
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

	@Operation(description = "Returns a map of execution ID to names by the provided ids.")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/search/names/by/ids")
	@Secured(right="execution-read")
	public Map<String, String> getExecutionsNamesByIds(List<String> ids) {
		return executionAccessor.findByIds(ids).collect(Collectors.toMap(e -> e.getId().toHexString(), Execution::getDescription));
	}

	@Operation(description = "Returns the last execution triggered by a specific task.")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/search/last/by/task-id/{taskId}")
	@Secured(right="execution-read")
	public List<Execution> getLastExecutionsByTaskId(
			@PathParam("taskId") String taskId,
			@QueryParam("limit") int limit,
			@QueryParam("from") Long from,
			@QueryParam("to") Long to) {
		return executionAccessor.getLastEndedExecutionsBySchedulerTaskID(taskId, limit, from, to);
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
			try (Stream<ReportNode> reportNodesByExecutionID = getContext().getReportAccessor().getReportNodesByExecutionIDAndClass(executionID, reportNodeClass, limit)) {
				result = reportNodesByExecutionID.collect(Collectors.toList());
			}
		} else {
			try (Stream<ReportNode> reportNodesByExecutionID = getContext().getReportAccessor().getReportNodesByExecutionID(executionID, limit)) {
				result = reportNodesByExecutionID.collect(Collectors.toList());
			}
		}
		return result;
	}

	@Operation(description = "Returns the list of report nodes with contributing errors for the given execution")
	@GET
	@Path("/{id}/reportnodes-with-errors")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right = "execution-read")
	public List<ReportNode> getReportNodeWithContributingErrors(@PathParam("id") String executionId, @QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
		skip = skip != null ? skip : 0;
		limit = limit != null ? limit : 1000;
		Stream<ReportNode> stream = getContext().getReportAccessor().getReportNodesWithContributingErrors(executionId, skip, limit);
		return stream.collect(Collectors.toList());
	}

	@Operation(description = "Returns the list of report nodes by execution id and artefact hash")
	@GET
	@Path("/{id}/reportnodes-by-hash/{hash}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right = "execution-read")
	public List<ReportNode> getReportNodesByArtefactHash(@PathParam("id") String executionId, @PathParam("hash") String hash, @QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
		skip = skip != null ? skip : 0;
		limit = limit != null ? limit : 1000;
		try (Stream<ReportNode> stream = getContext().getReportAccessor().getReportNodesByArtefactHash(executionId, hash, skip, limit)) {
			return stream.collect(Collectors.toList());
		}
	}

	@Operation(description = "Returns the full aggregated report view for the provided execution.")
	@GET
	@Path("/{id}/report/aggregated")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right = "execution-read")
	public AggregatedReportView getFullAggregatedReportView(@PathParam("id") String executionId) {
		return getAggregatedReportView(executionId, new AggregatedReportViewBuilder.AggregatedReportViewRequest(null, null));
	}

	@Operation(description = "Returns an aggregated report view for the provided execution and aggregation parameters.")
	@POST
	@Path("/{id}/report/aggregated")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right = "execution-read")
	public AggregatedReportView getAggregatedReportView(@PathParam("id") String executionId, AggregatedReportViewBuilder.AggregatedReportViewRequest request) {
		ExecutionEngineContext executionEngineContext = getScheduler().getExecutor().getExecutionEngine().getExecutionEngineContext();
		AggregatedReportViewBuilder aggregatedReportViewBuilder = new AggregatedReportViewBuilder(executionEngineContext, executionId);
		return aggregatedReportViewBuilder.buildAggregatedReportView(request);
	}

	@Operation(description = "Returns the custom report for the execution")
	@GET
	@Path("/{id}/report/{customReportType}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Secured(right = "execution-read")
	public Response getCustomReport(@PathParam("id") String executionId,
									@PathParam("customReportType") String customReportType,
									@QueryParam("includeAttachments") Boolean includeAttachments,
									@QueryParam("attachmentsRootFolder") String attachmentsRootFolder) throws IOException {
		ExecutionEngineContext executionEngineContext = getScheduler().getExecutor().getExecutionEngine().getExecutionEngineContext();
		CustomReportType reportType = CustomReportType.parse(customReportType);
		if (reportType == null) {
			throw getControllerServiceException(customReportType);
		}
		switch (reportType) {
			case JUNITXML:
				if (includeAttachments != null && includeAttachments) {
					throw new ControllerServiceException(400, "Attachments are not supported in " + CustomReportType.JUNITXML + " report");
				}
				return createJUnitXmlReport(executionEngineContext, List.of(executionId));
			case JUNITZIP:
				return createJUnitZipReport(executionEngineContext, List.of(executionId), includeAttachments, attachmentsRootFolder);
			default:
				throw getControllerServiceException(customReportType);
		}
	}

	@Operation(description = "Returns the custom report for several executions")
	@GET
	@Path("/report/multi/{customReportType}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@Secured(right = "execution-read")
	public Response getCustomMultiReport(@PathParam("customReportType") String customReportType,
										 @QueryParam("ids") String executionIds,
										 @QueryParam("includeAttachments") Boolean includeAttachments,
										 @QueryParam("attachmentsRootFolder") String attachmentsRootFolder) throws IOException {
		ExecutionEngineContext executionEngineContext = getScheduler().getExecutor().getExecutionEngine().getExecutionEngineContext();
		CustomReportType reportType = CustomReportType.parse(customReportType);
		if (reportType == null) {
			throw getControllerServiceException(customReportType);
		}
		List<String> idsList = new ArrayList<>();
		if (executionIds != null) {
			idsList = Arrays.asList(executionIds.split(";"));
		}
		switch (reportType) {
			case JUNITXML:
				if (includeAttachments != null && includeAttachments) {
					throw new ControllerServiceException(400, "Attachments are not supported in " + CustomReportType.JUNITXML + " report");
				}
				return createJUnitXmlReport(executionEngineContext, idsList);
			case JUNITZIP:
				return createJUnitZipReport(executionEngineContext, idsList, includeAttachments, attachmentsRootFolder);
			default:
				throw getControllerServiceException(customReportType);
		}
	}

	private static ControllerServiceException getControllerServiceException(String customReportType) {
		return new ControllerServiceException(400, "Invalid report type: " + customReportType + ". Supported report types: " + Arrays.toString(CustomReportType.values()));
	}

	private Response createJUnitXmlReport(ExecutionEngineContext executionEngineContext, List<String> executionIds) throws IOException {
		JUnitReport junitReport = new JUnitXmlReportBuilder(executionEngineContext).buildJUnitXmlReport(executionIds);
		Response.ResponseBuilder response = Response.ok(new ByteArrayInputStream(junitReport.getContent()));
		response.header("Content-Disposition", "attachment; filename=\"" + junitReport.getFileName() + "\"");
		return response.build();
	}

	private Response createJUnitZipReport(ExecutionEngineContext executionEngineContext, List<String> executionIds, Boolean includeAttachments, String attachmentsRootFolder) throws IOException {
		JUnitReport junitReport = new JUnitXmlReportBuilder(executionEngineContext).buildJunitZipReport(executionIds, includeAttachments, attachmentsRootFolder);
		Response.ResponseBuilder response = Response.ok(new ByteArrayInputStream(junitReport.getContent()));
		response.header("Content-Disposition", "attachment; filename=\"" + junitReport.getFileName() + "\"");
		return response.build();
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
	@Secured(right="plan-bulk-execute")
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
	@Secured(right="plan-bulk-execute")
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
