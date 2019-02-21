package step.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.commons.helpers.FileHelper;
import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;

@Path("/resources")
public class ResourceServices extends AbstractServices { 

	private static final Logger logger = LoggerFactory.getLogger(ResourceServices.class);

	protected ResourceManager resourceManager;
	protected ResourceAccessor resourceAccessor;
	
	@PostConstruct
	public void init() {
		resourceManager = getContext().get(ResourceManager.class);
		resourceAccessor = getContext().get(ResourceAccessor.class);
	}
	
	public static class ResourceUploadResponse {
		
		protected Resource resource;
		protected List<Resource> similarResources;

		public ResourceUploadResponse() {
			super();
		}

		public ResourceUploadResponse(Resource resource, List<Resource> similarResources) {
			super();
			this.resource = resource;
			this.similarResources = similarResources;
		}
		
		@JsonIgnore
		public String getResourceId() {
			return resource.getId().toString();
		}

		public Resource getResource() {
			return resource;
		}
		
		public void setResource(Resource resource) {
			this.resource = resource;
		}
		
		public List<Resource> getSimilarResources() {
			return similarResources;
		}

		public void setSimilarResources(List<Resource> similarResources) {
			this.similarResources = similarResources;
		}
	}

	@POST
	@Secured
	@Path("/content")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public ResourceUploadResponse createResource(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail, @QueryParam("type") String resourceType) throws IOException {
		if (uploadedInputStream == null || fileDetail == null)
			throw new RuntimeException("Invalid arguments");
		if (resourceType == null || resourceType.length() == 0)
			throw new RuntimeException("Missing resource type query parameter 'type'");
		
		try {
			Resource resource = resourceManager.createResource(resourceType, uploadedInputStream, fileDetail.getFileName(), true);
			return new ResourceUploadResponse(resource, null);
		} catch (SimilarResourceExistingException e) {
			return new ResourceUploadResponse(e.getResource(), e.getSimilarResources());
		}
	}
	
	@POST
	@Secured
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Resource saveResource(Resource resource) {
		return resourceAccessor.save(resource);
	}
	
	@POST
	@Secured
	@Path("/{id}/content")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public ResourceUploadResponse saveResourceContent(@PathParam("id") String resourceId, @FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {
		if (uploadedInputStream == null || fileDetail == null)
			throw new RuntimeException("Invalid arguments");
		
		Resource resource = resourceManager.saveResourceContent(resourceId, uploadedInputStream, fileDetail.getFileName());
		return new ResourceUploadResponse(resource, null);
	}
	
	@GET
	@Secured
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Resource getResource(@PathParam("id") String resourceId) throws IOException {
		return resourceAccessor.get(new ObjectId(resourceId));
	}
	
	@GET
	@Secured
	@Path("/{id}/content")
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
	@Secured
    @Path("/revision/{id}/content")
	public Response getResourceRevisionContent(@PathParam("id") String resourceRevisionId, @QueryParam("inline") boolean inline) throws IOException {
		ResourceRevisionContentImpl resourceContent = resourceManager.getResourceRevisionContent(resourceRevisionId);
		return getResponseForResourceRevisionContent(resourceContent, inline);
	}
	
	@javax.ws.rs.core.Context 
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
				mimeType = "text/plain";
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
