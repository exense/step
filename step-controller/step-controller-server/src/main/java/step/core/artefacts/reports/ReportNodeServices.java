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
package step.core.artefacts.reports;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.execution.ExecutionContext;

@Singleton
@Path("reportnodes")
public class ReportNodeServices extends AbstractServices {

	protected ReportNodeAccessor reportNodeAccessor;
	
	@PostConstruct
	public void init() {
		reportNodeAccessor = getContext().getReportAccessor();
	}
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Secured(right="execution-read")
	public ReportNode getReportNode(@PathParam("id") String reportNodeId) {
		return reportNodeAccessor.get(reportNodeId);
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="execution-write")
	public ReportNode save(ReportNode plan) {
		return reportNodeAccessor.save(plan);
	}
	
	@POST
	@Path("/many")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="execution-write")
	public void saveMany(List<ReportNode> reportNodes) {
		reportNodes.forEach(r->{
			System.out.println("ReportNode:"+r.getId().toString());
			String executionID = r.getExecutionID();
			ExecutionContext executionRunnable = getExecutionRunnable(executionID);
			executionRunnable.getExecutionCallbacks().afterReportNodeExecution(executionRunnable, r);
			reportNodeAccessor.save(r);
		});
	}
	
}
