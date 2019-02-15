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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import step.attachments.AttachmentContainer;
import step.commons.datatable.DataTable;
import step.commons.datatable.TableRow;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.ArtefactRegistry;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionRunnable;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.RepositoryObjectReference;
import step.core.repositories.TestSetStatusOverview;
import step.core.scheduler.ExecutiontTaskParameters;
import step.grid.client.GridClient;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;

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
	public void execute(ExecutiontTaskParameters schedule) {
		getScheduler().addExecutionTask(schedule);
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
	@Path("/execution")
	@Secured(right="plan-execute")
	public String execute(ExecutionParameters executionParams) {
		String executionID = getScheduler().execute(executionParams);
		return executionID;
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
	
//	@POST
//	@Path("/execution/{id}/report")
//	@Consumes(MediaType.APPLICATION_JSON)
//	@Produces(MediaType.APPLICATION_JSON)
//	public ReportExport reportExecution(@PathParam("id") String executionID, RepositoryObjectReference report) {
//		return getContext().getRepositoryObjectManager().exportTestExecutionReport(report, executionID);
//	}
	
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
	public void abort(@PathParam("id") String executionID) {
		ExecutionRunnable task = getExecutionRunnable(executionID);
		if(task!=null) {
			task.getExecutionLifecycleManager().abort();
		}
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
	
	@GET
	@Path("/artefact/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public AbstractArtefact getArtefact(@PathParam("id") String id) {
		return getContext().getArtefactAccessor().get(new ObjectId(id));
	}
	
	@POST
	@Path("/artefact/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public AbstractArtefact searchArtefactByAttributes(Map<String, String> attributes) {
		return getContext().getArtefactAccessor().findByAttributes(attributes);
	}

	
	@POST
	@Path("/artefact/{id}/attributes")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public AbstractArtefact saveArtefactAttributes(Map<String, String> attributes, @PathParam("id") String id) {
		ArtefactAccessor accessor = getContext().getArtefactAccessor();
		AbstractArtefact artefact = accessor.get(id);
		artefact.setAttributes(attributes);
		return accessor.save(artefact);
	}
	
	@POST
	@Path("/artefact")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public AbstractArtefact saveArtefact(AbstractArtefact artefact) {
		return getContext().getArtefactAccessor().save(artefact);
	}
	
	@POST
	@Path("/artefacts")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void saveArtefact(List<AbstractArtefact> artefact) {
		getContext().getArtefactAccessor().save(artefact);
	}
	
	
	@GET
	@Path("/artefact/{id}/descendants")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-read")
	public ArtefactTree getArtefactDescendants(@PathParam("id") String id) {
		ArtefactAccessor a = getContext().getArtefactAccessor();
		AbstractArtefact root = a.get(id);
		ArtefactTree rootNode = new ArtefactTree(root);
		getChildrenRecursive(a, rootNode);
		return rootNode;
	}
	
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
		Class<? extends AbstractArtefact> clazz = ArtefactRegistry.getInstance().getArtefactType(type);		
		AbstractArtefact sample = clazz.newInstance();
		for(Method m:clazz.getMethods()) {
			if(m.getAnnotation(PostConstruct.class)!=null) {
				m.invoke(sample);
			}
		}
		
		getContext().getArtefactAccessor().save(sample);
		return sample;
	}
	
	public class ArtefactTree {
		
		AbstractArtefact artefact;
		
		List<ArtefactTree> children;

		public ArtefactTree(AbstractArtefact artefact) {
			super();
			this.artefact = artefact;
		}

		public AbstractArtefact getArtefact() {
			return artefact;
		}

		public List<ArtefactTree> getChildren() {
			return children;
		}

		public void setChildren(List<ArtefactTree> children) {
			this.children = children;
		}
	}
	
	private void getChildrenRecursive(ArtefactAccessor a, ArtefactTree current) {
		AbstractArtefact parent = current.getArtefact();
		if(parent.getChildrenIDs()!=null) {
			List<ArtefactTree> childrenNodes = new ArrayList<>();
			for(ObjectId childId:parent.getChildrenIDs()) {
				AbstractArtefact child = a.get(childId);
				ArtefactTree childNode = new ArtefactTree(child);
				childrenNodes.add(childNode);
				getChildrenRecursive(a, childNode);
			}
			current.setChildren(childrenNodes);
		}
	}
	
	@POST
	@Path("/artefact/{id}/children")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public AbstractArtefact addChild(@PathParam("id") String id, AbstractArtefact child) {
		ArtefactAccessor a = getContext().getArtefactAccessor();
		
		child = a.save(child);
		
		AbstractArtefact artefact = a.get(id);
		artefact.addChild(child.getId());
		
		a.save(artefact);
		
		return child;
	}
	
	@DELETE
	@Path("/artefact/{id}/children/{childid}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void removeChild(@PathParam("id") String parentid, @PathParam("childid") String childid) {
		ArtefactAccessor a = getContext().getArtefactAccessor();
		AbstractArtefact artefact = a.get(parentid);
		artefact.removeChild(new ObjectId(childid));
		a.save(artefact);
	}
	
	@POST
	@Path("/artefact/{id}/children/{childid}/move")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void moveChildUp(@PathParam("id") String parentid, @PathParam("childid") String childid, int offset) {
		ArtefactAccessor a = getContext().getArtefactAccessor();
		AbstractArtefact artefact = a.get(parentid);
		
		ObjectId child = new ObjectId(childid);
		int pos = artefact.indexOf(child);
		int newPos = pos+offset;
		if(newPos>=0&&newPos<artefact.getChildrenIDs().size()) {
			artefact.removeChild(child);
			artefact.add(newPos, child);		
		}
		a.save(artefact);
	}
	
	@POST
	@Path("/artefact/{id}/move")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void moveArtefact(@PathParam("id") String id, @QueryParam("from") String originParentId, 
			@QueryParam("to") String targetParentId, @QueryParam("pos") int newPosition) {
		ArtefactAccessor a = getContext().getArtefactAccessor();
		AbstractArtefact origin = a.get(originParentId);
		origin.removeChild(new ObjectId(id));
		a.save(origin);
		
		AbstractArtefact target = a.get(targetParentId);
		target.add(newPosition, new ObjectId(id));		
		a.save(target);
	}
	
	@POST
	@Path("/artefact/{id}/copy")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void copyArtefact(@PathParam("id") String id, 
			@QueryParam("to") String targetParentId, @QueryParam("pos") int newPosition) {
		getContext().getArtefactManager().copyArtefact(id, targetParentId);
	}
	
	@POST
	@Path("/artefact/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void updateArtefact(AbstractArtefact artefact) {
		getContext().getArtefactAccessor().save(artefact);
	}
	
	@DELETE
	@Path("/artefact/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-delete")
	public void deleteArtefact(@PathParam("id") String id) {
		ArtefactAccessor a = getContext().getArtefactAccessor();
		removeRecursive(new ObjectId(id), a);
	}

	private void removeRecursive(ObjectId id, ArtefactAccessor a) {
		AbstractArtefact artefact = a.get(id);
		if(artefact!=null) {
			if(artefact.getChildrenIDs()!=null) {
				for(ObjectId childId:artefact.getChildrenIDs()) {
					removeRecursive(childId, a);
				}
			}
			a.remove(id);
		}
	}
}
