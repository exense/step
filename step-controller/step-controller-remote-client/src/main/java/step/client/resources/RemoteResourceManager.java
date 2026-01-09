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
import step.core.objectenricher.ObjectPredicate;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.resources.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

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
		return upload(bodyPart, ResourceManager.RESOURCE_TYPE_STAGING_CONTEXT_FILES, false, null, null, null);
	}
	
	protected ResourceUploadResponse upload(FormDataBodyPart bodyPart, String type, boolean isDirectory, String trackingAttribute, String origin, Long originTimestamp) {
		MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(bodyPart);
        
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
		params.put("directory", Boolean.toString(isDirectory));
		if (trackingAttribute != null) {
			params.put("trackingAttribute", trackingAttribute);
		}
		if (origin != null) {
			params.put("origin", origin);
		}
		if (originTimestamp != null) {
			params.put("originTimestamp", Long.toString(originTimestamp));
		}
		// in RemoteResourceManager we ignore actor user, because in remote services he will be automatically resolved from authentication context
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
								   ObjectEnricher objectEnricher, String actorUser) throws IOException, InvalidResourceFormatException {
		return createResource(resourceType, false, resourceStream, resourceFileName, objectEnricher, actorUser);
	}

	@Override
	public Resource createResource(String resourceType, boolean isDirectory, InputStream resourceStream, String resourceFileName,
								   ObjectEnricher objectEnricher, String actorUser) throws IOException, InvalidResourceFormatException {
		return createTrackedResource(resourceType, isDirectory, resourceStream, resourceFileName, objectEnricher, null, actorUser, null, null);
	}

	@Override
	public Resource createTrackedResource(String resourceType, boolean isDirectory, InputStream resourceStream, String resourceFileName, ObjectEnricher objectEnricher,
										  String trackingAttribute, String actorUser, String origin, Long originTimestamp) throws IOException, InvalidResourceFormatException {
		return createTrackedResource(resourceType, isDirectory, resourceStream, resourceFileName, null, objectEnricher, trackingAttribute, actorUser, origin, originTimestamp);
	}

	@Override
	public Resource createTrackedResource(String resourceType, boolean isDirectory, InputStream resourceStream, String resourceFileName,
										  String optionalResourceName, ObjectEnricher objectEnricher,
										  String trackingAttribute, String actorUser, String origin, Long originTimestamp) {
		StreamDataBodyPart bodyPart = new StreamDataBodyPart("file", resourceStream, resourceFileName);

		ResourceUploadResponse upload = upload(bodyPart, resourceType, isDirectory, trackingAttribute, origin, originTimestamp);
		return upload.getResource();
	}

	@Override
	public Resource copyResource(Resource resource, ResourceManager sourceResourceManager, String actorUser) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Resource saveResourceContent(Resource resource, InputStream resourceStream, String resourceFileName, String optionalResourceName, String actorUser)
			throws IOException {

		throw new RuntimeException("Not implemented");
	}

	@Override
	public Resource saveResourceContent(String resourceId, InputStream resourceStream, String resourceFileName, String optionalResourceName, String actorUser)
			throws IOException {

		throw new RuntimeException("Not implemented");
	}

	@Override
	public void deleteResource(String resourceId) {
		Builder b = requestBuilder("/rest/resources/"+resourceId);
		executeRequest(()-> b.delete());
	}

	@Override
	public void deleteResourceRevisionContent(String resourceId) {
		Builder b = requestBuilder("/rest/resources/"+resourceId +"/revisions");
		executeRequest(()-> b.delete());
	}

	@Override
	public List<Resource> findManyByCriteria(Map<String, String> criteria) {
		Builder b = requestBuilder("/rest/resources/find");
		Entity<Map<String, String>> entity = Entity.entity(criteria, MediaType.APPLICATION_JSON);
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
	public Resource getResourceByNameAndType(String resourceName, String resourceType, ObjectPredicate predicate) {
		throw new RuntimeException("Not implemented");
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
	public ResourceRevisionFileHandle getResourceFile(String resourceId, String revisionId) {
		Resource resource = getResource(resourceId);

		Builder b = requestBuilder("/rest/resources/revision/"+revisionId+"/content");
		byte[] content = executeRequest(()-> b.get(byte[].class));
		File container = new File("resources/" + resourceId + "/" +  revisionId);
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
	public ResourceRevisionContainer createResourceContainer(String resourceType, String resourceFileName, String actorUser)
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

	@Override
	public void findAndCleanupUnusedRevision(String resourceId, Set<String> usedRevision) {
		throw new RuntimeException("Not implemented");
	}
}
