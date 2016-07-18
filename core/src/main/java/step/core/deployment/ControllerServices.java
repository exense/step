package step.core.deployment;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionRunnable;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.execution.model.ReportExport;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.RepositoryObjectManager;
import step.core.repositories.RepositoryObjectReference;
import step.core.repositories.TestSetStatusOverview;
import step.core.scheduler.ExecutiontTaskParameters;

@Singleton
@Path("controller")
public class ControllerServices extends AbstractServices {

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/task")
	public void execute(ExecutiontTaskParameters schedule) {
		getScheduler().addExecutionTask(schedule);
	}
	
	@PUT
	@Path("/task/{id}")
	public void enableExecutionTask(@PathParam("id") String executionTaskID) {
		getScheduler().enableExecutionTask(executionTaskID);
	}
	
	@DELETE
	@Path("/task/{id}")
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
	public ExecutiontTaskParameters getExecutionTask(@PathParam("id") String executionTaskID) {
		return getScheduler().get(executionTaskID);
	}
	
	@GET
	@Path("/task")
	@Produces(MediaType.APPLICATION_JSON)
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
	@Path("/execution")
	public String execute(ExecutionParameters executionParams) {
		String executionID = getScheduler().execute(executionParams);
		return executionID;
	}
	
	@GET
	@Path("/execution/{id}")
	@Produces(MediaType.APPLICATION_JSON)
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
	public RTMLink getRtmLink(@PathParam("id") String executionID) {
		RTMLink link = new RTMLink();
//		link.link = RTMLinkGenerator.getAggregateViewByEid(executionID);
		return link;
	}	
	
	@POST
	@Path("/execution/{id}/report")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ReportExport reportExecution(@PathParam("id") String executionID, RepositoryObjectReference report) {
		return getContext().getRepositoryObjectManager().exportTestExecutionReport(report, executionID);
	}
	
	@GET
	@Path("/execution/{id}/statusdistribution")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<ReportNodeStatus, Integer> getStatusReport(@PathParam("id") String executionID, @QueryParam("class") String reportNodeClass) {
		return getContext().getReportAccessor().getLeafReportNodesStatusDistribution(executionID, reportNodeClass);
	}
	
	@GET
	@Path("/execution/{id}/throughput")
	@Produces({"application/json;response-pass-through=true"})
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
	public void abort(@PathParam("id") String executionID) {
		ExecutionRunnable task = getExecutionRunnable(executionID);
		if(task!=null) {
			getContext().getExecutionLifecycleManager().abort(task);
		}
	}
	
	@GET
	@Path("/reportnode/{id}")
	public ReportNode getReportNode(@PathParam("id") String reportNodeId) {
		return getContext().getReportAccessor().get(new ObjectId(reportNodeId));
	}
	
	@GET
	@Path("/reportnode/{id}/path")
	public List<ReportNodeAndArtefact> getReportNodePath(@PathParam("id") String reportNodeId) {
		List<ReportNodeAndArtefact> result = new ArrayList<>();
		ArtefactAccessor artefactAccessor = getContext().getArtefactAccessor();
		List<ReportNode> path = getContext().getReportAccessor().getReportNodePath(new ObjectId(reportNodeId));
		path.forEach((node) -> result.add(new ReportNodeAndArtefact(node, node.getArtefactID()!=null?artefactAccessor.get(node.getArtefactID()):null)));
		return result;
	}
	
	class ReportNodeAndArtefact {
		
		ReportNode reportNode;
		
		AbstractArtefact artefact;

		public ReportNodeAndArtefact() {
			super();
		}

		public ReportNodeAndArtefact(ReportNode reportNode,	AbstractArtefact artefact) {
			this.reportNode = reportNode;
			this.artefact = artefact;
		}

		public ReportNode getReportNode() {
			return reportNode;
		}

		public void setReportNode(ReportNode reportNode) {
			this.reportNode = reportNode;
		}

		public AbstractArtefact getArtefact() {
			return artefact;
		}

		public void setArtefact(AbstractArtefact artefact) {
			this.artefact = artefact;
		}
	}

	@GET
	@Path("/executions")
	@Produces(MediaType.APPLICATION_JSON)
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
	public ArtefactInfo getArtefactInfo(RepositoryObjectReference ref) {
		try {
			return RepositoryObjectManager.executeRequest(ref, "/artefact/info", ArtefactInfo.class);
		} catch (Exception e) {
			 throw new WebApplicationException(Response.status(500).entity("Unable to retrieve artefact.").type("text/plain").build());
		}
	}
	
	@POST
	@Path("/repository/report")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public TestSetStatusOverview getReport(RepositoryObjectReference report) {
		return RepositoryObjectManager.getReport(report);
	}
	
	@GET
	@Path("/artefact/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public AbstractArtefact getArtefact(@PathParam("id") String id) {
		return getContext().getArtefactAccessor().get(new ObjectId(id));
	}
	
	@POST
	@Path("/artefact/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void updateArtefact(AbstractArtefact artefact) {
		getContext().getArtefactAccessor().save(artefact);
	}
	
	@DELETE
	@Path("/artefact/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void removeArtefact(@PathParam("id") String id) {
		getContext().getArtefactAccessor().remove(new ObjectId(id));
	}
	
}
