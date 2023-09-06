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
package step.client.resources;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.objectenricher.ObjectEnricher;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.resources.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides an API to upload resources (compiled code of a keyword, etc) to the controller 
 *
 */
public class RemoteResourceManager extends AbstractRemoteClient implements ResourceManager {
	
	private static final Logger logger = LoggerFactory.getLogger(RemoteResourceManager.class);
	
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
		return upload(bodyPart, ResourceManager.RESOURCE_TYPE_STAGING_CONTEXT_FILES, false, true, null);
	}
	
	protected ResourceUploadResponse upload(FormDataBodyPart bodyPart, String type, boolean isDirectory, boolean checkForDuplicates, String trackingAttribute) {
		MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(bodyPart);
        
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
		params.put("directory", Boolean.toString(isDirectory));
        params.put("duplicateCheck", Boolean.toString(checkForDuplicates));
		if (trackingAttribute != null) {
			params.put("trackingAttribute", trackingAttribute);
		}
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
		Builder b1 = requestBuilder("/rest/resources/"+resourceId);
		Resource resource = executeRequest(()-> b1.get(Resource.class));
		
		if(resource != null) {
			Builder b2 = requestBuilder("/rest/resources/"+resourceId+"/content");
			return executeRequest(()-> AttachmentHelper.generateAttachmentFromByteArray(b2.get(byte[].class), resource.getResourceName()));
		} else {
			return null;
		}
	}

	@Override
	public Resource createResource(String resourceType, InputStream resourceStream, String resourceFileName,
			boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException {
		return createResource(resourceType, false, resourceStream, resourceFileName, checkForDuplicates, objectEnricher);
	}

	@Override
	public Resource createResource(String resourceType, boolean isDirectory, InputStream resourceStream, String resourceFileName,
								   boolean checkForDuplicates, ObjectEnricher objectEnricher) {
		return createResource(resourceType, isDirectory, resourceStream, resourceFileName, checkForDuplicates, objectEnricher, null);
	}

	@Override
	public Resource createResource(String resourceType, boolean isDirectory, InputStream resourceStream, String resourceFileName, boolean checkForDuplicates, ObjectEnricher objectEnricher, String trackingAttribute) {
		StreamDataBodyPart bodyPart = new StreamDataBodyPart("file", resourceStream, resourceFileName);

		// !!! in fact, 'checkForDuplicates' parameter is ignored, because the list of found duplicated resources (with the same hash sums)
		// is located in ResourceUploadResponse.similarResources, but we ignore this list here and just take the uploaded resource
		ResourceUploadResponse upload = upload(bodyPart, resourceType, isDirectory, checkForDuplicates, trackingAttribute);
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
	public List<Resource> findManyByAttributes(Map<String, String> attributes) {
		Builder b = requestBuilder("/rest/resources/find");
		Entity<Map<String, String>> entity = Entity.entity(attributes, MediaType.APPLICATION_JSON);
		return executeRequest(() -> b.post(entity, new GenericType<List<Resource>>() {}));
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
			logger.error("Error while copying resource content to file "+file.getAbsolutePath(), e);
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
	public ResourceRevisionContentImpl getResourceRevisionContent(String resourceRevisionId) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public ResourceRevision getResourceRevision(String resourceRevisionId) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public ResourceRevisionContainer createResourceContainer(String resourceType, String resourceFileName)
			throws IOException {
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

	@Override
	public String getResourcesRootPath() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public ResourceRevision saveResourceRevision(ResourceRevision resourceRevision) throws IOException {
		throw new RuntimeException("Not implemented");
	}

}
