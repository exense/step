package step.core.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.AttachmentContainer;
import step.attachments.AttachmentManager;

@Path("/upload")
public class UploadServices extends AbstractServices {

	AttachmentManager attachmentManager;
	
	private static final Logger logger = LoggerFactory.getLogger(UploadServices.class);
	
	@PostConstruct
	public void init() {
		attachmentManager = getContext().getAttachmentManager();
	}
	
	public static class UploadResponse {
		
		String attachmentId;

		public UploadResponse() {
			super();
		}

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
		String uploadedFileLocation = container.getContainer() + "/" + fileDetail.getName();
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

}
