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

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import step.artefacts.CallPlan;
import step.artefacts.handlers.PlanLocator;
import step.artefacts.handlers.SelectorHelper;
import step.core.Controller;
import step.core.GlobalContext;
import step.core.Version;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.RepositoryObjectReference;
import step.core.repositories.TestSetStatusOverview;
import step.framework.server.security.Secured;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static step.core.Controller.USER_ACTIVITY_MAP_KEY;

@Singleton
@Path("controller")
@Tag(name = "Controller")
public class ControllerServices extends AbstractStepServices {

	private Version currentVersion;
	private PlanLocator planLocator;
	private ObjectPredicate objectPredicate;
	private Controller controller;
	private ExecutionAccessor executionAccessor;
	private Map<String, Long> userActivityMap;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		currentVersion = context.getCurrentVersion();
		controller = context.require(Controller.class);

		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(getContext().getExpressionHandler()));
		SelectorHelper selectorHelper = new SelectorHelper(dynamicJsonObjectResolver);
		planLocator = new PlanLocator(getContext().getPlanAccessor(), selectorHelper);
		objectPredicate = context.get(ObjectHookRegistry.class).getObjectPredicate(getSession());

		executionAccessor = context.getExecutionAccessor();
		userActivityMap = (Map<String, Long>) context.get(USER_ACTIVITY_MAP_KEY);
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/shutdown")
	@Secured(right="controller-manage")
	public void shutdown() {
		new Thread(() -> controller.destroy()).start();
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
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public List<ReportNode> getReportNodePath(@PathParam("id") String reportNodeId) {
		// Forced to convert result to ArrayList to avoid error in Jackson
		List<ReportNode> result = new ArrayList<>();
		List<ReportNode> path = getContext().getReportAccessor().getReportNodePath(new ObjectId(reportNodeId));
		path.forEach((node) -> result.add(node));
		return result;
	}

	@GET
	@Path("/reportnode/{id}/plan")
	@Produces(MediaType.APPLICATION_JSON)
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
			if (resolvedArtefact instanceof CallPlan) {
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
	@Path("/version")
	public Version getVersion() {
		return this.currentVersion;
	}

	private static final Pattern JAR_FILENAME_PATTERN = Pattern.compile("([^\\\\/]+)-(\\d+\\.\\d+\\.?[^.]+).jar");

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/lib/versions")
	public Map<String, String> getLibVersions() {
		HashMap<String, String> result = new HashMap<>();
		Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)).forEach(e -> {
			Matcher matcher = JAR_FILENAME_PATTERN.matcher(e);
			if (matcher.find()) {
				String lib = matcher.group(1);
				String version = matcher.group(2);
				result.put(lib, version);
			}
		});
		return result;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/status")
	public Status getControllerStatus() {
		Status status = new Status();
		List<Execution> activeTests = executionAccessor.getActiveTests();
		status.activeTests = activeTests.size();
		status.lastExecutionEndTime = -1;
		Iterator<Execution> iterator = executionAccessor.findLastEnded(1).iterator();
		status.lastExecutionEndTime = (iterator.hasNext()) ? iterator.next().getEndTime() : -1;
		status.lastUserActivityTime = userActivityMap.values().stream().mapToLong(v -> v).max().orElse(-1);

		//If test are currently running idle time is 0
		if (status.activeTests > 0) {
			status.idleTimeMs = 0;
		} else {
			//idle time is either latest end time of executions or last logged-in user activity
			long lastActivity = (status.lastUserActivityTime > status.lastExecutionEndTime) ?
					status.lastUserActivityTime : status.lastExecutionEndTime;
			status.idleTimeMs = System.currentTimeMillis() - lastActivity;
		}
		return status;
	}


	private class Status {
		public long idleTimeMs;
		public long activeTests;
		public long lastExecutionEndTime;
		public long lastUserActivityTime;
	}
}
