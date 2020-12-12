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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.objectenricher.ObjectEnricher;
import step.resources.LocalResourceManagerImpl;
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
public class CachedRemoteResourceManager extends AbstractRemoteClient implements ResourceManager {
	
	private static final Logger logger = LoggerFactory.getLogger(CachedRemoteResourceManager.class);
	private final LocalResourceManagerImpl localResourceManager = new LocalResourceManagerImpl(new File("resourcemanager/"+UUID.randomUUID().toString())) {

		@Override
		protected void closeResourceContainer(Resource resource, ResourceRevision resourceRevision, boolean checkForDuplicates,
				ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException {
			super.closeResourceContainer(resource, resourceRevision, checkForDuplicates, objectEnricher);
			
		}
		
	};
	private final RemoteResourceManager remoteResourceManager;
	
	public CachedRemoteResourceManager() {
		super();
		remoteResourceManager = new RemoteResourceManager();
	}

	public CachedRemoteResourceManager(ControllerCredentials credentials) {
		remoteResourceManager = new RemoteResourceManager(credentials);
	}
		
	private ResourceUploadResponse upload(FormDataBodyPart bodyPart, String type, boolean checkForDuplicates) {
		MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(bodyPart);
        
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        params.put("duplicateCheck", Boolean.toString(checkForDuplicates));
        Builder b = requestBuilder("/rest/resources/content", params);
        return executeRequest(()->b.post(Entity.entity(multiPart, multiPart.getMediaType()), ResourceUploadResponse.class));
	}

	@Override
	public Resource createResource(String resourceType, InputStream resourceStream, String resourceFileName,
			boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException {
		return localResourceManager.createResource(resourceType, resourceStream, resourceFileName, checkForDuplicates, objectEnricher);
	}

	@Override
	public Resource saveResourceContent(String resourceId, InputStream resourceStream, String resourceFileName)
			throws IOException {
		return localResourceManager.saveResourceContent(resourceId, resourceStream, resourceFileName);
	}

	@Override
	public void deleteResource(String resourceId) {
		localResourceManager.deleteResource(resourceId);
	}

	@Override
	public ResourceRevisionContent getResourceContent(String resourceId) throws IOException {
		Resource resource = localResourceManager.getResource(resourceId);
		ResourceRevisionContent resourceContent;
		if(resource != null) {
			resourceContent = localResourceManager.getResourceContent(resourceId);
		} else {
			resourceContent = remoteResourceManager.getResourceContent(resourceId);
		}
		return resourceContent;
	}

	public Resource getResource(String resourceId) {
		Resource resource = localResourceManager.getResource(resourceId);
		if(resource != null)  {
			return resource;
		} else {
			return remoteResourceManager.getResource(resourceId);
		}
	}

	@Override
	public ResourceRevisionFileHandle getResourceFile(String resourceId) {
		throw new RuntimeException("Not implemented");
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
		return localResourceManager.createResourceContainer(resourceType, resourceFileName);
	}

	@Override
	public Resource lookupResourceByName(String resourcename) {
		Resource resource = localResourceManager.lookupResourceByName(resourcename);
		if(resource != null) {
			return resource;
		} else {
			return remoteResourceManager.lookupResourceByName(resourcename);
		}
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
	public Resource updateResourceContent(Resource resource, InputStream resourceStream, String resourceFileName,
			ResourceRevision revision) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public ResourceRevision saveResourceRevision(ResourceRevision resourceRevision) throws IOException {
		throw new RuntimeException("Not implemented");
	}

}
