package step.client.resources;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.objectenricher.ObjectEnricher;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceRevision;
import step.resources.ResourceRevisionContainer;
import step.resources.ResourceRevisionContent;
import step.resources.ResourceRevisionContentImpl;
import step.resources.ResourceRevisionFileHandle;
import step.resources.ResourceUploadResponse;
import step.resources.SimilarResourceExistingException;

/**
 * This class provides an API to upload resources (compiled code of a keyword, etc) to the controller 
 *
 */
public class RemoteResourceManager extends AbstractRemoteClient implements ResourceManager {

	public RemoteResourceManager() {
		super();
	}

	public RemoteResourceManager(ControllerCredentials credentials) {
		super(credentials);
	}

	/**
	 * Upload the local file provided as argument to the controller
	 * 
	 * @param file the local file to be uploaded
	 * @return the {@link ResourceUploadResponse} containing an handle to the uploaded file
	 */
	public ResourceUploadResponse upload(File file) {
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        return upload(fileDataBodyPart);

	}
	
	/**
	 * Upload the local file provided as argument to the controller
	 * 
	 * @param filename the path to the local file to be uploaded
	 * @return the {@link ResourceUploadResponse} containing an handle to the uploaded file
	 */
	public ResourceUploadResponse upload(String filename, InputStream stream) {
        StreamDataBodyPart bodyPart = new StreamDataBodyPart("file", stream, filename);
        return upload(bodyPart);
	}

	protected ResourceUploadResponse upload(FormDataBodyPart bodyPart) {
		return upload(bodyPart, ResourceManager.RESOURCE_TYPE_STAGING_CONTEXT_FILES, true);
	}
	
	protected ResourceUploadResponse upload(FormDataBodyPart bodyPart, String type, boolean checkForDuplicates) {
		MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(bodyPart);
        
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        params.put("duplicateCheck", Boolean.toString(checkForDuplicates));
        Builder b = requestBuilder("/rest/resources/content", params);
        return executeRequest(()->b.post(Entity.entity(multiPart, multiPart.getMediaType()), ResourceUploadResponse.class));
	}

	/**
	 * Download a resource based on its id
	 * 
	 * @param resourceId the id of the resource to be downloaded
	 * @return the {@link Response} containing an handle to the uploaded file
	 */
	public Attachment download(String resourceId) {
		Builder b = requestBuilder("/rest/resources/"+resourceId+"/content");
        return executeRequest(()-> AttachmentHelper.generateAttachmentFromByteArray(b.get(byte[].class), "unnamed"));
	}

	@Override
	public Resource createResource(String resourceType, InputStream resourceStream, String resourceFileName,
			boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException {
		 StreamDataBodyPart bodyPart = new StreamDataBodyPart("file", resourceStream, resourceFileName);
		ResourceUploadResponse upload = upload(bodyPart, resourceType, checkForDuplicates);
		return upload.getResource();
	}

	@Override
	public Resource saveResourceContent(String resourceId, InputStream resourceStream, String resourceFileName)
			throws IOException {
		
		return null;
	}

	@Override
	public void deleteResource(String resourceId) {
		Builder b = requestBuilder("/rest/resources/"+resourceId);
		executeRequest(()-> b.delete());
	}

	@Override
	public ResourceRevisionContent getResourceContent(String resourceId) throws IOException {
		Resource resource = getResource(resourceId);
		
		Builder b = requestBuilder("/rest/resources/"+resourceId+"/content");
		return new ResourceRevisionContent() {
			
			@Override
			public InputStream getResourceStream() {
				InputStream in = (InputStream) b.get().getEntity();
				return in;
			}
			
			@Override
			public String getResourceName() {
				return resource.getResourceName();
			}
			
			@Override
			public void close() throws IOException {
				// TODO Auto-generated method stub
				
			}
		};
	}

	public Resource getResource(String resourceId) {
		Builder b = requestBuilder("/rest/resources/"+resourceId);
		Resource resource = executeRequest(()->b.get(Resource.class));
		return resource;
	}

	@Override
	public ResourceRevisionFileHandle getResourceFile(String resourceId) {
		Resource resource = getResource(resourceId);
		
		Builder b = requestBuilder("/rest/resources/"+resourceId+"/content");
		byte[] content = executeRequest(()-> b.get(byte[].class));
		File container = new File("resources/"+resourceId);
		container.mkdirs();
		File file = new File(container.getAbsolutePath()+"/"+resource.getResourceName());
		try {
			Files.copy(new ByteArrayInputStream(content), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new ResourceRevisionFileHandle() {
			
			@Override
			public File getResourceFile() {
				return file;
			}
			
			@Override
			public void close() throws IOException {
			}
		};
	}

	@Override
	public ResourceRevision getResourceRevisionByResourceId(String resourceId) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public ResourceRevisionContentImpl getResourceRevisionContent(String resourceRevisionId) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public ResourceRevisionContainer createResourceContainer(String resourceType, String resourceFileName)
			throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Resource lookupResourceByName(String resourcename) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean resourceExists(String resourceId) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Resource saveResource(Resource resource) throws IOException {
		throw new RuntimeException("Not implemented");
	}

}
