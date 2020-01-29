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
package step.core.deployment;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import step.commons.datatable.DataTable;
import step.commons.datatable.TableRow;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactRegistry;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionRunnable;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessorImpl;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.RepositoryObjectReference;
import step.core.repositories.TestSetStatusOverview;
import step.core.scheduler.ExecutiontTaskParameters;

@Singleton
@Path("controller")
public class ControllerServices extends AbstractServices {
	
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
	@Path("/task")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="task-read")
	public List<ExecutiontTaskParameters> getScheduledExecutions() {
		List<ExecutiontTaskParameters> result = new ArrayList<ExecutiontTaskParameters>();
		Iterator<ExecutiontTaskParameters> it = getScheduler().getActiveAndInactiveExecutionTasks();
		int maxSize = 50;
		while(it.hasNext() && result.size()<maxSize) {
			result.add(it.next());
		}
		return result;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/executions/findByCritera")
	@Secured(right="execution-write")
	public List<Execution> findByCritera(FindByCriteraParam param) {
		return ((ExecutionAccessorImpl) getContext().getExecutionAccessor()).findByCritera(param.getCriteria(), 
				param.getStart().getTime(), param.getEnd().getTime(), 
				param.getSkip(), param.getLimit());
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/execution")
	@Secured(right="plan-execute")
	public String execute(ExecutionParameters executionParams) {
		String executionID = getScheduler().execute(executionParams);
		return executionID;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/save/execution")
	@Secured(right="plan-execute")
	public void saveExecution(Execution execution) {
		getContext().getExecutionAccessor().save(execution);
	}
	
	@GET
	@Path("/execution/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="report-read")
	public Execution getExecution(@PathParam("id") String executionID) {
		return getContext().getExecutionAccessor().get(executionID);
	}	
	
	public class RTMLink {
		String link;
		public String getLink() {
			return link;
		}
	}
		
	@GET
	@Path("/execution/{id}/rtmlink")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="report-read")
	public RTMLink getRtmLink(@PathParam("id") String executionID) {
		RTMLink link = new RTMLink();
//		link.link = RTMLinkGenerator.getAggregateViewByEid(executionID);
		return link;
	}	
	
	@POST
	@Path("/executions/byref")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="report-read")
	public List<Execution> getExecutionsByRepositoryObjectReference(RepositoryObjectReference objectReference) {
		List<Execution> executionsByArtefactURL = getContext().getExecutionAccessor().getTestExecutionsByArtefactURL(objectReference);
		return executionsByArtefactURL;
	}
	
	@GET
	@Path("/execution/{id}/statusdistribution")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="report-read")
	public Map<ReportNodeStatus, Integer> getStatusReport(@PathParam("id") String executionID, @QueryParam("class") String reportNodeClass) {
		return getContext().getReportAccessor().getLeafReportNodesStatusDistribution(executionID, reportNodeClass);
	}
	
	@GET
	@Path("/execution/{id}/throughput")
	@Produces({"application/json;response-pass-through=true"})
	@Secured(right="report-read")
	public DataTable getStatusReport(@PathParam("id") String executionID, @QueryParam("resolution") Integer nInterval)  {
		Execution e = getContext().getExecutionAccessor().get(executionID);
		
		long t2;
		if(e.getStatus()==ExecutionStatus.ENDED) {
			t2=e.getEndTime();
		} else {
			t2=System.currentTimeMillis();
		}
		
		long duration = t2 - e.getStartTime();
	
		int resolution = (int) (1.0*duration/nInterval);
		
		DataTable t = getContext().getReportAccessor().getTimeBasedReport(executionID, resolution);
		
		if(t.getRows().size()<resolution) {
			long time = t.getRows().size()>0?t.getRows().get(t.getRows().size()-1).getDate().getTime():e.getStartTime();
			for(int i=t.getRows().size();i<nInterval;i++) {
				time+=resolution;
				t.addRow(new TableRow(new Date(time), 0.0));
			}
		}
		
		return t;
	}
	
	@GET
	@Path("/execution/{id}/reportnodes")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="report-read")
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
	
	@GET
	@Path("/execution/{id}/stop")
	@Secured(right="plan-execute")
	public Void abort(@PathParam("id") String executionID) {
		ExecutionRunnable task = getExecutionRunnable(executionID);
		if(task!=null) {
			task.getExecutionLifecycleManager().abort();
		}
		return null;
	}
	
	@GET
	@Path("/reportnode/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="report-read")
	public ReportNode getReportNode(@PathParam("id") String reportNodeId) {
		return getContext().getReportAccessor().get(new ObjectId(reportNodeId));
	}
	
	@GET
	@Path("/reportnode/{id}/path")
	@Secured(right="report-read")
	public List<ReportNode> getReportNodePath(@PathParam("id") String reportNodeId) {
		List<ReportNode> result = new ArrayList<>();
		List<ReportNode> path = getContext().getReportAccessor().getReportNodePath(new ObjectId(reportNodeId));
		path.forEach((node) -> result.add(node));
		return result;
	}
	
	@GET
	@Path("/reportnode/{id}/children")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="report-read")
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

	@GET
	@Path("/executions")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="report-read")
	public List<Execution> getExecutions(@QueryParam("limit") int limit) {		
		List<Execution> result = new ArrayList<>();
		for(Execution e:getContext().getExecutionAccessor().findLastStarted(limit)) {
			result.add(e);
		}
		return result;
	}
	
	@POST
	@Path("/repository/artefact/info")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="report-read")
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
	@Secured(right="report-read")
	public TestSetStatusOverview getReport(RepositoryObjectReference report) throws Exception {
		return getContext().getRepositoryObjectManager().getReport(report);
	}
	
//	@GET
//	@Path("/artefact/lookupPlan/{id}")
//	@Consumes(MediaType.APPLICATION_JSON)
//	@Produces(MediaType.APPLICATION_JSON)
//	@Secured(right="plan-read")
//	public AbstractArtefact lookupPlan(@PathParam("id") String id) {
//		throw new RuntimeException();
		// TODO implement
//		AbstractArtefact planArtefact=null;
//		try {
//			ArtefactAccessor accessor = getContext().getPlanAccessor();
//			CallPlan artefact = (CallPlan) accessor.get(id);
//			DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(getContext().getExpressionHandler()));
//			SelectorHelper selectorHelper = new SelectorHelper(dynamicJsonObjectResolver);
//			PlanLocator planLocator = new PlanLocator(null,accessor,selectorHelper);
//			return planLocator.selectPlan(artefact);
//		} catch (RuntimeException e) {}
//		return planArtefact;
//	}

	
	@GET
	@Path("/artefact/types")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public Set<String> getArtefactTypes() {
		return ArtefactRegistry.getInstance().getArtefactNames();
	}
	
	@GET
	@Path("/artefact/types/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public AbstractArtefact getArtefactType(@PathParam("id") String type) throws Exception {
		return ArtefactRegistry.getInstance().getArtefactTypeInstance(type);
	}
}