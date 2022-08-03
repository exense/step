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

import step.core.artefacts.reports.ReportNode;
import step.core.collections.SearchOrder;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.FindByCriteraParam;
import step.framework.server.security.Secured;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionAccessorImpl;
import step.core.execution.model.ExecutionParameters;
import step.core.repositories.RepositoryObjectReference;
import step.engine.execution.ExecutionLifecycleManager;

@Singleton
@Path("executions")
@Tag(name = "Executions")
public class ExecutionServices extends AbstractStepServices {

	protected ExecutionAccessor executionAccessor;
	
	@PostConstruct
	public void init() {
		executionAccessor = getContext().getExecutionAccessor();
	}

	@Operation(description = "Starts an execution with the given parameters.")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/start")
	@Secured(right="plan-execute")
	public String execute(ExecutionParameters executionParams) {
		return getScheduler().execute(executionParams);
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

	@Operation(description = "Returns the executions matching the provided attributes.")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/search")
	@Secured(right="execution-read")
	public Execution getExecutionByAttribute(Map<String,String> attributes) {
		return executionAccessor.findByAttributes(attributes);
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
			iterator =  getContext().getReportAccessor().getReportNodesByExecutionIDAndClass(executionID, reportNodeClass);
		} else {
			iterator =  getContext().getReportAccessor().getReportNodesByExecutionID(executionID);
		}
		int i = 0;
		while(iterator.hasNext()&&i<limit) {
			i++;
			result.add(iterator.next());
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

	@Operation(description = "Delete the execution with the given execution id.")
	@DELETE
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="execution-delete")
	public void deleteExecution(@PathParam("id") String id) {
		executionAccessor.remove(new ObjectId(id));
	}
}
