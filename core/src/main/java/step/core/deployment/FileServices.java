package step.core.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.AttachmentContainer;
import step.attachments.AttachmentManager;

@Path("/files")
public class FileServices extends AbstractServices {

	AttachmentManager attachmentManager;
	
	private static final Logger logger = LoggerFactory.getLogger(FileServices.class);
	
	@PostConstruct
	public void init() {
		attachmentManager = getContext().getAttachmentManager();
	}
	
	public static class UploadResponse {
		
		String attachmentId;

		public UploadResponse() {
			super();
		}

		/**
		 * @return an handle to the uploaded file
		 */
		public String getAttachmentId() {
			return attachmentId;
		}

		public void setAttachmentId(String attachmentId) {
			this.attachmentId = attachmentId;
		}
	}
	
	@POST
	@Secured
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public UploadResponse uploadFile(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) {
		
		if (uploadedInputStream == null || fileDetail == null)
			throw new RuntimeException("Invalid arguments");

		AttachmentContainer container =  attachmentManager.createAttachmentContainer();
		String uploadedFileLocation = container.getContainer() + "/" + fileDetail.getFileName();
		try {
			Files.copy(uploadedInputStream, Paths.get(uploadedFileLocation));
		} catch (IOException e) {
			logger.error("Error while saving file "+uploadedFileLocation, e);
			throw new RuntimeException("Error while saving file.");
		}
		UploadResponse response = new UploadResponse();
		response.setAttachmentId(container.getMeta().getId().toString());
		return response;
	}
	
	@GET
	@Secured
    @Path("/{id}")
	public Response downloadFile(@PathParam("id") String id) {
		File file = attachmentManager.getFileById(id);

		StreamingOutput fileStream = new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException {
				java.nio.file.Path path = file.toPath();
				byte[] data = Files.readAllBytes(path);
				output.write(data);
				output.flush();
			}
		};
		return Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
				.header("content-disposition", "attachment; filename = "+file.getName()).build();
	}
	
	@GET
	@Secured
    @Path("/{id}/name")
	public String getFilename(@PathParam("id") String id) {
		File file = attachmentManager.getFileById(id);
		return file.getName();
	}

}
