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
package step.resources;

import ch.exense.commons.io.FileHelper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import step.core.GlobalContext;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.ControllerServiceException;
import step.core.objectenricher.ObjectEnricher;
import step.framework.server.security.Secured;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Path("/resources")
@Tag(name = "Resources")
public class ResourceServices extends AbstractStepServices {

	protected ResourceManager resourceManager;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext globalContext = getContext();
		resourceManager = globalContext.getResourceManager();
	}
	
	@POST
	@Secured
	@Path("/content")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public ResourceUploadResponse createResource(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail, @QueryParam("type") String resourceType,
												 @QueryParam("duplicateCheck") Boolean checkForDuplicate, @QueryParam("directory") Boolean isDirectory,
												 @QueryParam("trackingAttribute") String trackingAttribute) throws IOException {
		ObjectEnricher objectEnricher = getObjectEnricher();
		
		if(checkForDuplicate == null) {
			checkForDuplicate = true;
		}
		if (uploadedInputStream == null || fileDetail == null)
			throw new RuntimeException("Invalid arguments");
		if (resourceType == null || resourceType.length() == 0)
			throw new RuntimeException("Missing resource type query parameter 'type'");
		
		try {
			Resource resource = resourceManager.createResource(resourceType, isDirectory, uploadedInputStream, fileDetail.getFileName(), checkForDuplicate, objectEnricher, trackingAttribute);
			return new ResourceUploadResponse(resource, null);
		} catch (SimilarResourceExistingException e) {
			return new ResourceUploadResponse(e.getResource(), e.getSimilarResources());
		} catch (InvalidResourceFormatException e) {
			throw uploadFileNotAnArchive();
		}
	}

	private ControllerServiceException uploadFileNotAnArchive() {
		return new ControllerServiceException("The uploaded file is not an archive. Please upload a zip of the folder.");
	}

	@POST
	@Secured
	//@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Resource saveResource(Resource resource) throws IOException {
		return resourceManager.saveResource(resource);
	}
	
	@POST
	@Path("/{id}/content")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public ResourceUploadResponse saveResourceContent(@PathParam("id") String resourceId, @FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) throws Exception {
		if (uploadedInputStream == null || fileDetail == null)
			throw new RuntimeException("Invalid arguments");

		try {
			Resource resource = resourceManager.saveResourceContent(resourceId, uploadedInputStream, fileDetail.getFileName() );
			return new ResourceUploadResponse(resource, null);
		} catch (InvalidResourceFormatException e) {
			throw uploadFileNotAnArchive();
		}
	}
	
	@GET
	@Secured
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Resource getResource(@PathParam("id") String resourceId) throws IOException {
		return resourceManager.getResource(resourceId);
	}
	
	@GET
	@Path("/{id}/content")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getResourceContent(@PathParam("id") String resourceId, @QueryParam("inline") boolean inline) throws IOException {
		ResourceRevisionContent resourceContent = resourceManager.getResourceContent(resourceId);
		return getResponseForResourceRevisionContent(resourceContent, inline);
	}
	
	@DELETE
	@Secured
	@Path("/{id}")
	public void deleteResource(@PathParam("id") String resourceId) {
		resourceManager.deleteResource(resourceId);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    @Path("/revision/{id}/content")
	public Response getResourceRevisionContent(@PathParam("id") String resourceRevisionId, @QueryParam("inline") boolean inline) throws IOException {
		ResourceRevisionContentImpl resourceContent = resourceManager.getResourceRevisionContent(resourceRevisionId);
		return getResponseForResourceRevisionContent(resourceContent, inline);
	}

	@POST
	@Path("/find")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public List<Resource> findManyByCriteria(Map<String, String> criteria) {
		return resourceManager.findManyByCriteria(criteria);
	}
	
	@jakarta.ws.rs.core.Context 
	ServletContext context;
	
	protected Response getResponseForResourceRevisionContent(ResourceRevisionContent resourceContent, boolean inline) {
		StreamingOutput fileStream = new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException {
				FileHelper.copy(resourceContent.getResourceStream(), output, 2048);
				resourceContent.close();
			}
		};
		
		String resourceName = resourceContent.getResourceName();
		String mimeType = context.getMimeType(resourceName);
		if (mimeType == null) {
			if(resourceName.endsWith(".log")) {
				mimeType = "text/plain; charset=utf-8";
			} else {
				mimeType = "application/octet-stream";
			}
		}
		
		String contentDisposition;
		if(inline) {
			contentDisposition = "inline";
		} else {
			contentDisposition = "attachment";
		}
		
		String headerValue = String.format(contentDisposition+"; filename=\"%s\"", resourceName);
		
		return Response.ok(fileStream, mimeType)
				.header("content-disposition", headerValue).build();
	}
}
