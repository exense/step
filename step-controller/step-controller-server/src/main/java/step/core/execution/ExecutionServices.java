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

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.deployment.AbstractServices;
import step.core.deployment.FindByCriteraParam;
import step.core.deployment.Secured;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionAccessorImpl;
import step.core.repositories.RepositoryObjectReference;

@Singleton
@Path("executions")
public class ExecutionServices extends AbstractServices {

	protected ExecutionAccessor executionAccessor;
	
	@PostConstruct
	public void init() {
		executionAccessor = getContext().getExecutionAccessor();
	}
	
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
	
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public Execution get(@PathParam("id") String id) {
		return executionAccessor.get(id);
	}
	
	@POST
	@Path("/search")
	@Secured(right="execution-read")
	public Execution get(Map<String,String> attributes) {
		return executionAccessor.findByAttributes(attributes);
	}
	
	@POST
	@Path("/search/by/ref")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public List<Execution> getExecutionsByRepositoryObjectReference(RepositoryObjectReference objectReference) {
		List<Execution> executionsByArtefactURL = getContext().getExecutionAccessor().getTestExecutionsByArtefactURL(objectReference);
		return executionsByArtefactURL;
	}
	
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/search/by/critera")
	@Secured(right="execution-read")
	public List<Execution> findByCritera(FindByCriteraParam param) {
		return ((ExecutionAccessorImpl) getContext().getExecutionAccessor()).findByCritera(param.getCriteria(), 
				param.getStart().getTime(), param.getEnd().getTime(), 
				param.getSkip(), param.getLimit());
	}
	
	@GET
	@Path("/{id}/statusdistribution")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public Map<ReportNodeStatus, Integer> getStatusReport(@PathParam("id") String executionID, @QueryParam("class") String reportNodeClass) {
		return getContext().getReportAccessor().getLeafReportNodesStatusDistribution(executionID, reportNodeClass);
	}
	
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

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="execution-write")
	public Execution save(Execution execution) {
		return executionAccessor.save(execution);
	}
	
	@DELETE
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="execution-delete")
	public void delete(@PathParam("id") String id) {
		executionAccessor.remove(new ObjectId(id));
	}
}
