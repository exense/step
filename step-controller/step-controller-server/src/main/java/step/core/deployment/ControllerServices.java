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
package step.core.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bson.types.ObjectId;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.RepositoryObjectReference;
import step.core.repositories.TestSetStatusOverview;
import step.core.scheduler.ExecutiontTaskParameters;
import step.engine.execution.ExecutionLifecycleManager;

@Singleton
@Path("controller")
public class ControllerServices extends AbstractServices {
	
	private ArtefactHandlerRegistry artefactHandlerRegistry;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		artefactHandlerRegistry = getContext().getArtefactHandlerRegistry();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/shutdown")
	@Secured(right="admin")
	public void shutdown() {
		new Thread() {
			@Override
			public void run() {
				controller.destroy();
			}
		}.start();;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/task")
	@Secured(right="task-write")
	public void schedule(ExecutiontTaskParameters schedule) {
		// Enrich the execution parameters with the attributes of the task parameters.
		// The attributes of the execution parameters are then added to the Execution
		// This is for instance needed to run the execution within the same project as
		// the scheduler task
		getObjectEnricher().accept(schedule.getExecutionsParameters());
		getScheduler().addExecutionTask(schedule);
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/task/{id}/execute")
	@Secured(right="plan-execute")
	public String execute(@PathParam("id") String executionTaskID) {
		Session session = getSession();
		return getScheduler().executeExecutionTask(executionTaskID, session.getUser().getUsername());
	}
	
	@PUT
	@Path("/task/schedule")
	@Secured(right="admin")
	public void enableAllExecutionTasksSchedule(@QueryParam("enabled") Boolean enabled) {
		if(enabled != null && enabled) {
			getScheduler().enableAllExecutionTasksSchedule();
		} else {
			getScheduler().disableAllExecutionTasksSchedule();
		}
	}
	
	
	@PUT
	@Path("/task/{id}")
	@Secured(right="task-write")
	public void enableExecutionTask(@PathParam("id") String executionTaskID) {
		getScheduler().enableExecutionTask(executionTaskID);
	}
	
	@DELETE
	@Path("/task/{id}")
	@Secured(right="task-delete")
	public void removeExecutionTask(@PathParam("id") String executionTaskID, @QueryParam("remove") Boolean remove) {
		if(remove!=null && remove) {
			getScheduler().removeExecutionTask(executionTaskID);
		} else {
			getScheduler().disableExecutionTask(executionTaskID);
		}
	}
	
	@GET
	@Path("/task/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="task-read")
	public ExecutiontTaskParameters getExecutionTask(@PathParam("id") String executionTaskID) {
		return getScheduler().get(executionTaskID);
	}
	
	@GET
	@Path("/task/new")
	@Produces(MediaType.APPLICATION_JSON)
	@Unfiltered
	@Secured(right="task-write")
	public ExecutiontTaskParameters createExecutionTask(@PathParam("id") String executionTaskID) {
		ExecutiontTaskParameters taskParameters = new ExecutiontTaskParameters();
		taskParameters.setActive(true);
		ExecutionParameters executionsParameters = new ExecutionParameters();
		HashMap<String, String> repositoryParameters = new HashMap<>();
		executionsParameters.setRepositoryObject(new RepositoryObjectReference("local", repositoryParameters));
		executionsParameters.setMode(ExecutionMode.RUN);
		taskParameters.setExecutionsParameters(executionsParameters);
		return taskParameters;
	}
	
	@GET
	@Path("/task")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="task-read")
	public List<ExecutiontTaskParameters> getScheduledExecutions() {
		List<ExecutiontTaskParameters> result = new ArrayList<ExecutiontTaskParameters>();
		Iterator<ExecutiontTaskParameters> it = getScheduler().getActiveAndInactiveExecutionTasks();
		int maxSize = getContext().getConfiguration().getPropertyAsInteger("tec.services.tasks.maxsize", 500);
		while(it.hasNext() && result.size()<maxSize) {
			result.add(it.next());
		}
		return result;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/execution")
	@Secured(right="plan-execute")
	public String execute(ExecutionParameters executionParams) {
		String executionID = getScheduler().execute(executionParams);
		return executionID;
	}
	
	@GET
	@Path("/execution/{id}/stop")
	@Secured(right="plan-execute")
	public Void abort(@PathParam("id") String executionID) {
		ExecutionContext context = getExecutionRunnable(executionID);
		if(context!=null) {
			new ExecutionLifecycleManager(context).abort();
		}
		return null;
	}

	@GET
	@Path("/reportnode/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public ReportNode getReportNode(@PathParam("id") String reportNodeId) {
		return getContext().getReportAccessor().get(new ObjectId(reportNodeId));
	}
	
	@GET
	@Path("/reportnode/{id}/path")
	@Secured(right="execution-read")
	public List<ReportNode> getReportNodePath(@PathParam("id") String reportNodeId) {
		List<ReportNode> result = new ArrayList<>();
		List<ReportNode> path = getContext().getReportAccessor().getReportNodePath(new ObjectId(reportNodeId));
		path.forEach((node) -> result.add(node));
		return result;
	}
	
	@GET
	@Path("/reportnode/{id}/children")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public List<ReportNode> getReportNodeChildren(@PathParam("id") String reportNodeId, @QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
		skip = skip!=null?skip:0;
		limit = limit!=null?limit:1000;
		
		List<ReportNode> result = new ArrayList<>();
		Iterator<ReportNode> it = getContext().getReportAccessor().getChildren(new ObjectId(reportNodeId), skip, limit);
		while(it.hasNext()) {
			result.add(it.next());
		}		
		return result;
	}

	@POST
	@Path("/repository/artefact/info")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public ArtefactInfo getArtefactInfo(RepositoryObjectReference ref) {
		try {
			return getContext().getRepositoryObjectManager().getArtefactInfo(ref);
		} catch (Exception e) {
			throw new WebApplicationException(Response.status(500).entity("Unable to retrieve artefact."+e.getMessage()).type("text/plain").build());
		}
	}
	
	@POST
	@Path("/repository/report")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public TestSetStatusOverview getReport(RepositoryObjectReference report) throws Exception {
		return getContext().getRepositoryObjectManager().getReport(report);
	}
	
	@GET
	@Path("/artefact/types")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public Set<String> getArtefactTypes() {
		return artefactHandlerRegistry.getArtefactNames();
	}

	@GET
	@Path("/artefact/templates")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public Set<String> getArtefactTemplates() {
		return new TreeSet<>(artefactHandlerRegistry.getArtefactTemplateNames());
	}
	
	@GET
	@Path("/artefact/types/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Unfiltered
	@Secured(right="plan-read")
	public AbstractArtefact getArtefactType(@PathParam("id") String type) throws Exception {
		return artefactHandlerRegistry.getArtefactTypeInstance(type);
	}
}
