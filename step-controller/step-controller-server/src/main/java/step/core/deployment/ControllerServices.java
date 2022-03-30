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
import java.util.Objects;
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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.types.ObjectId;

import step.artefacts.CallPlan;
import step.artefacts.handlers.PlanLocator;
import step.artefacts.handlers.SelectorHelper;
import step.core.GlobalContext;
import step.core.Version;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.ObjectPredicateFactory;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.RepositoryObjectReference;
import step.core.repositories.TestSetStatusOverview;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.tasks.AsyncTaskManager;
import step.engine.execution.ExecutionLifecycleManager;

@Singleton
@Path("controller")
@Tag(name = "Controller")
public class ControllerServices extends AbstractServices {
	
	private ArtefactHandlerRegistry artefactHandlerRegistry;
	private AsyncTaskManager taskManager;
	private Version currentVersion;
	private PlanLocator planLocator;
	private ObjectPredicate objectPredicate;
			
	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		artefactHandlerRegistry = context.getArtefactHandlerRegistry();
		taskManager = context.get(AsyncTaskManager.class);
		currentVersion = context.getCurrentVersion();

		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(getContext().getExpressionHandler()));
		SelectorHelper selectorHelper = new SelectorHelper(dynamicJsonObjectResolver);
		planLocator = new PlanLocator(getContext().getPlanAccessor(), selectorHelper);
		objectPredicate = context.get(ObjectPredicateFactory.class).getObjectPredicate(getSession());
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
	@Path("/reportnode/{id}/plan")
	@Secured(right="execution-read")
	public Plan getReportNodeRootPlan(@PathParam("id") String reportNodeId) {
		PlanAccessor planAccessor = getContext().getPlanAccessor();
		ReportNode reportNode = getContext().getReportAccessor().get(reportNodeId);
		Plan plan = planAccessor.get(getContext().getExecutionAccessor().get(reportNode.getExecutionID()).getPlanId());
		if (reportNode.getParentID() != null) {
			CallPlan callPlan = getParentCallPlan(reportNode.getParentID().toString());
			if (callPlan != null) {
				Plan locatedPlan = planLocator.selectPlan(callPlan, objectPredicate, null);
				plan = (locatedPlan != null) ? locatedPlan : plan;
			}
		}
		return plan;
	}
	
	private CallPlan getParentCallPlan(String nodeId) {
		ReportNode reportNode = (nodeId != null) ? getContext().getReportAccessor().get(nodeId): null;
		if (reportNode != null) {
			AbstractArtefact resolvedArtefact = reportNode.getResolvedArtefact();
			if (resolvedArtefact != null && resolvedArtefact instanceof CallPlan) {
				//get related plan
				return (CallPlan) resolvedArtefact;
			} else if (reportNode.getParentID() != null) {
				return getParentCallPlan(reportNode.getParentID().toString());
			}
		}
		return null;
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
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/async-task/{id}")
	@Secured()
	public AsyncTaskManager.TaskStatus getAsyncTaskStatus(@PathParam("id") String asyncTaskId) {
		AsyncTaskManager.TaskStatus status = taskManager.getTaskStatus(asyncTaskId);
		return status;
	}


	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/version")
	public Version getVersion() {
		return this.currentVersion;
	}
}
