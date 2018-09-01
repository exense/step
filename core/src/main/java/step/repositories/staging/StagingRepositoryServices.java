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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

import step.attachments.AttachmentContainer;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.repositories.RepositoryObjectReference;
import step.repositories.staging.StagingContextRegistry.StagingContextImpl;

@Singleton
@Path("staging")
public class StagingRepositoryServices extends AbstractServices {
	
	protected StagingContextRegistry registry;
	
	@PostConstruct
	public void init() {
		registry = getContext().get(StagingContextRegistry.class);
	}
	
	@GET
	@Path("/context")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public String createContext() {
		String id = UUID.randomUUID().toString();
		StagingContextImpl context = new StagingContextImpl(id);
		registry.put(id, context);
		return id;
	}
	
	@POST
	@Path("/context/{id}/plan")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public void uploadPlan(@PathParam("id") String id, Plan plan) {
		StagingContextImpl context = registry.get(id);
		context.setPlan(plan);
	}
	
	@POST
	@Path("/context/{id}/file")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public String uploadFile(@PathParam("id") String id, @FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) {
		StagingContextImpl context = registry.get(id);
		if (uploadedInputStream == null || fileDetail == null)
			throw new RuntimeException("Invalid arguments");

		AttachmentContainer container = controller.getContext().getAttachmentManager().createAttachmentContainer();
		File file = new File(container.getContainer()+"/"+fileDetail.getFileName());
		try {
			Files.copy(uploadedInputStream, file.toPath());
		} catch (IOException e) {
			throw new RuntimeException("Error while saving file.");
		}
		
		context.addAttachment(container.getMeta().getId().toString());
		return container.getMeta().getId().toString();
	}
	
	@POST
	@Path("/context/{id}/execute")
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="plan-write")
	public String execute(@PathParam("id") String id, Map<String, String> executionParameters, @QueryParam("isolate") boolean isolate) {
		StagingContextImpl context = registry.get(id);
		
		ExecutionParameters params = new ExecutionParameters();
		HashMap<String, String> repositoryParameters = new HashMap<>();
		repositoryParameters.put("contextid", id);
		
		params.setArtefact(new RepositoryObjectReference("local-isolated", repositoryParameters));
		params.setMode(ExecutionMode.RUN);
		params.setDescription("Remote test");
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
		StagingContextImpl context = registry.get(id);
		
		context.getAttachments().forEach(attachmentId->{
			controller.getContext().getAttachmentManager().deleteContainer(attachmentId);
		});
	}
}
