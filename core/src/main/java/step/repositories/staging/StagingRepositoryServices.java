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
package step.repositories.staging;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import step.commons.helpers.FileHelper;
import step.core.GlobalContext;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.repositories.RepositoryObjectReference;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionContainer;

@Singleton
@Path("staging")
public class StagingRepositoryServices extends AbstractServices {
	
	protected StagingContextAccessorImpl stagingContextAccessor ;
	protected ResourceManager resourceManager;
	
	@PostConstruct
	public void init() {
		GlobalContext context = getContext();
		stagingContextAccessor = context.get(StagingContextAccessorImpl.class);
		resourceManager = context.get(ResourceManager.class);
	}
	
	@GET
	@Path("/context")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public String createContext() {
		StagingContext context = new StagingContext();
		stagingContextAccessor.save(context);
		return context.getId().toString();
	}
	
	@POST
	@Path("/context/{id}/plan")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void uploadPlan(@PathParam("id") String id, Plan plan) {
		StagingContext context = stagingContextAccessor.get(id);
		context.setPlan(plan);
		stagingContextAccessor.save(context);
	}
	
	@POST
	@Path("/context/{id}/file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public String uploadFile(@PathParam("id") String id, @FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {
		StagingContext context = stagingContextAccessor.get(id);
		if (uploadedInputStream == null || fileDetail == null)
			throw new RuntimeException("Invalid arguments");

		ResourceRevisionContainer container = resourceManager.createResourceContainer("stagingContextFiles", fileDetail.getFileName());
		try {
			FileHelper.copy(uploadedInputStream, container.getOutputStream(), 2048);
		} catch (IOException e) {
			throw new RuntimeException("Error while saving file.");
		} finally {
			container.save();
		}
		
		String resourceId = container.getResource().getId().toString();
		context.addAttachment(resourceId);
		stagingContextAccessor.save(context);
		return resourceId;
	}
	
	@POST
	@Path("/context/{id}/execute")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public String execute(@PathParam("id") String id, Map<String, String> executionParameters, @QueryParam("isolate") boolean isolate) {
		StagingContext context = stagingContextAccessor.get(id);
		
		ExecutionParameters params = new ExecutionParameters();
		HashMap<String, String> repositoryParameters = new HashMap<>();
		repositoryParameters.put("contextid", id);
		
		params.setArtefact(new RepositoryObjectReference(StagingRepositoryPlugin.STAGING_REPOSITORY, repositoryParameters));
		params.setMode(ExecutionMode.RUN);
		params.setDescription(context.plan.getRoot().getAttributes().get("name"));
		params.setIsolatedExecution(isolate);
		// TODO replace by the real user
		params.setUserID("remote");
		params.setCustomParameters(executionParameters);
		
		return controller.getScheduler().execute(params);
	}
	
	@POST
	@Path("/context/{id}/destroy")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void destroy(@PathParam("id") String id) {
		StagingContext context = stagingContextAccessor.get(id);
		
		context.getAttachments().forEach(resourceId->{
			resourceManager.deleteResource(resourceId);
		});
	}
}
