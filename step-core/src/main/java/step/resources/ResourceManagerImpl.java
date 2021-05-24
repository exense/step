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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.exense.commons.core.model.resources.CustomResourceType;
import ch.exense.commons.core.model.resources.Resource;
import ch.exense.commons.core.model.resources.ResourceRevision;
import ch.exense.commons.core.model.resources.ResourceType;
import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;

import ch.exense.commons.io.FileHelper;
import step.core.objectenricher.ObjectEnricher;

public class ResourceManagerImpl implements ResourceManager {

	protected final File resourceRootFolder;
	protected final ResourceAccessor resourceAccessor;
	protected final ResourceRevisionAccessor resourceRevisionAccessor;
	protected final Map<String, ResourceType> resourceTypes;

	protected static final Logger logger = LoggerFactory.getLogger(ResourceManagerImpl.class);
	
	public ResourceManagerImpl(File resourceRootFolder, ResourceAccessor resourceAccessor,
			ResourceRevisionAccessor resourceRevisionAccessor) {
		super();
		this.resourceRootFolder = resourceRootFolder;
		this.resourceAccessor = resourceAccessor;
		this.resourceRevisionAccessor = resourceRevisionAccessor;
		this.resourceTypes = new ConcurrentHashMap<>();

		resourceTypes.put(RESOURCE_TYPE_TEMP, new CustomResourceType(true));
		resourceTypes.put(RESOURCE_TYPE_ATTACHMENT, new CustomResourceType(false));
		resourceTypes.put(RESOURCE_TYPE_STAGING_CONTEXT_FILES, new CustomResourceType(false));
		resourceTypes.put(RESOURCE_TYPE_FUNCTIONS, new CustomResourceType(false));
		resourceTypes.put(RESOURCE_TYPE_DATASOURCE, new CustomResourceType(false));
		resourceTypes.put(RESOURCE_TYPE_SECRET, new CustomResourceType(false));
		resourceTypes.put(RESOURCE_TYPE_PDF_TEST_SCENARIO_FILE, new CustomResourceType(false));

	}

	public void registerResourceType(String name, ResourceType resourceType) {
		resourceTypes.put(name, resourceType);
	}

	@Override
	public ResourceRevisionContainer createResourceContainer(String resourceType, String resourceFileName) throws IOException {
		Resource resource = createResource(resourceType, resourceFileName);
		ResourceRevision revision = createResourceRevision(resourceFileName, resource.getId().toString());
		createResourceRevisionContainer(resource, revision);
		File file = getResourceRevisionFile(resource, revision);
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		return new ResourceRevisionContainer(resource, revision, fileOutputStream, this);
	}

	protected void closeResourceContainer(Resource resource, ResourceRevision resourceRevision, boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException {
		File resourceRevisionFile = getResourceRevisionFile(resource, resourceRevision);
		String checksum = getMD5Checksum(resourceRevisionFile);
		resourceRevision.setChecksum(checksum);
		resourceRevisionAccessor.save(resourceRevision);

		resource.setCurrentRevisionId(resourceRevision.getId());
		
		if(objectEnricher != null) {
			objectEnricher.accept(resource);
		}
		resourceAccessor.save(resource);

		if(checkForDuplicates) {
			List<Resource> resourcesWithSameChecksum = getSimilarResources(resource, resourceRevision);
			if(resourcesWithSameChecksum.size()>0) {
				throw new SimilarResourceExistingException(resource, resourcesWithSameChecksum);
			}
		}
	}

	@Override
	public Resource createResource(String resourceType, InputStream resourceStream, String resourceFileName, boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException {
		ResourceRevisionContainer resourceContainer = createResourceContainer(resourceType, resourceFileName);
		FileHelper.copy(resourceStream, resourceContainer.getOutputStream(), 2048);
		resourceContainer.save(checkForDuplicates, objectEnricher);
		return resourceContainer.getResource();
	}

	@Override
	public Resource saveResourceContent(String resourceId, InputStream resourceStream, String resourceFileName) throws IOException {
		Resource resource = getResource(resourceId);
		createResourceRevisionAndSaveContent(resourceStream, resourceFileName, resource);
		updateResourceFileNameIfNecessary(resourceFileName, resource);
		return resource;
	}
	
	@Override
	public Resource updateResourceContent(Resource resource, InputStream resourceStream, String resourceFileName, ResourceRevision revision) throws IOException {
		saveResource(resource);
		updateContent(resourceStream, resourceFileName, resource, revision);
		return resource;
	}

	@Override
	public void deleteResource(String resourceId) {
		Resource resource = getResource(resourceId);

		resourceRevisionAccessor.getResourceRevisionsByResourceId(resourceId).forEachRemaining(revision->{
			resourceRevisionAccessor.remove(revision.getId());
		});

		File resourceContainer = getResourceContainer(resource);
		if(resourceContainer.exists()) {
			FileHelper.deleteFolder(resourceContainer);
		}

		resourceAccessor.remove(resource.getId());
	}

	private List<Resource> getSimilarResources(Resource actualResource, ResourceRevision actualResourceRevision) {
		List<Resource> result = new ArrayList<>();
		resourceRevisionAccessor.getResourceRevisionsByChecksum(actualResourceRevision.getChecksum()).forEachRemaining(revision->{
			if(!revision.getId().equals(actualResourceRevision.getId())) {
				Resource resource = resourceAccessor.get(new ObjectId(revision.getResourceId()));
				if(resource!=null) {
					 if (resource.getCurrentRevisionId() != null) {
						// ensure it is an active revision i.e a revision that is the current revision of a resource
						if(resource.getCurrentRevisionId().equals(revision.getId())) {
							try {
								if(FileUtils.contentEquals(getResourceRevisionFile(resource, revision), getResourceRevisionFile(actualResource, actualResourceRevision))) {
									result.add(resource);
								}
							} catch (IOException e) {
								logger.warn("Error while comparing resource revisions "+revision.getId()+" and "+actualResourceRevision.getId(), e);
							}
						}
					} else {
						logger.warn("Found resource without current revision: "+resource.getId());
					}
				} else {
					logger.warn("Found orphan resource revision: "+revision.getId());
				}
			}
		});
		return result;
	}

	@Override
	public ResourceRevisionContent getResourceContent(String resourceId) throws IOException {
		Resource resource = getResource(resourceId);
		ResourceRevision resourceRevision = getCurrentResourceRevision(resource);
		return getResourceRevisionContent(resource, resourceRevision);
	}


	protected void closeResourceRevisionContent(Resource resource) {
		if(resource.isEphemeral()) {
			deleteResource(resource.getId().toString());
		}
	}

	@Override
	public ResourceRevision getResourceRevisionByResourceId(String resourceId) {
		Resource resource = getResource(resourceId);
		ResourceRevision resourceRevision = getCurrentResourceRevision(resource);
		return resourceRevision;
	}

	@Override
	public ResourceRevisionContentImpl getResourceRevisionContent(String resourceRevisionId) throws IOException {
		ResourceRevision resourceRevision = getResourceRevision(new ObjectId(resourceRevisionId));
		Resource resource = getResource(resourceRevision.getResourceId());
		return getResourceRevisionContent(resource, resourceRevision);
	}

	@Override
	public ResourceRevisionFileHandle getResourceFile(String resourceId) {
		Resource resource = getResource(resourceId);
		ResourceRevision resourceRevision = getCurrentResourceRevision(resource);
		File resourceRevisionFile = getResourceRevisionFile(resource, resourceRevision);
		return new ResourceRevisionFileHandleImpl(this, resource, resourceRevisionFile);
	}

	private ResourceRevisionContentImpl getResourceRevisionContent(Resource resource, ResourceRevision resourceRevision)
			throws IOException {
		File resourceRevisionFile = getResourceRevisionFile(resource, resourceRevision);
		if(!resourceRevisionFile.exists() || !resourceRevisionFile.canRead()) {
			throw new IOException("The resource revision file "+resourceRevisionFile.getAbsolutePath()+" doesn't exist or cannot be read");
		}
		FileInputStream resourceRevisionStream = new FileInputStream(resourceRevisionFile);
		return new ResourceRevisionContentImpl(this, resource, resourceRevisionStream, resourceRevision.getResourceFileName());
	}

	private ResourceRevision getCurrentResourceRevision(Resource resource) {
		return getResourceRevision(resource.getCurrentRevisionId());
	}

	private ResourceRevision getResourceRevision(ObjectId resourceRevisionId) {
		ResourceRevision resourceRevision = resourceRevisionAccessor.get(resourceRevisionId);
		if(resourceRevision == null) {
			throw new RuntimeException("The resource revision with ID "+resourceRevisionId+" doesn't exist");
		}
		return resourceRevision;
	}

	@Override
	public Resource getResource(String resourceId) {
		Resource resource = resourceAccessor.get(new ObjectId(resourceId));
		if(resource == null) {
			throw new RuntimeException("The resource with ID "+resourceId+" doesn't exist");
		}
		return resource;
	}

	private File getResourceRevisionFile(Resource resource, ResourceRevision revision) {
		return new File(getResourceRevisionContainer(resource, revision).getPath()+"/"+revision.getResourceFileName());
	}

	private File getResourceRevisionContainer(Resource resource, ResourceRevision revision) {
		File containerFile = new File(getResourceContainer(resource).getPath()+"/"+revision.getId().toString());
		return containerFile;
	}

	private File getResourceContainer(Resource resource) {
		File containerFile = new File(resourceRootFolder+"/"+resource.getResourceType()+"/"+resource.getId().toString());
		return containerFile;
	}

	private File createResourceRevisionContainer(Resource resource, ResourceRevision revision) throws IOException {
		File containerFile = getResourceRevisionContainer(resource, revision);
		boolean containerDirectoryCreated = containerFile.mkdirs();
		if(!containerDirectoryCreated) {
			throw new IOException("Unable to create container for resource "+resource.getId()+" in "+containerFile.getAbsolutePath());
		}
		return containerFile;
	}

	private ResourceRevision createResourceRevisionAndSaveContent(InputStream resourceStream, String resourceFileName,
			Resource resource) throws IOException {
		ResourceRevision revision = createResourceRevision(resourceFileName, resource.getId().toString());
		createResourceRevisionContainer(resource, revision);

		File resourceFile = saveResourceRevisionContent(resourceStream, resource, revision);

		String checksum = getMD5Checksum(resourceFile);
		revision.setChecksum(checksum);
		resourceRevisionAccessor.save(revision);

		resource.setCurrentRevisionId(revision.getId());

		resourceAccessor.save(resource);
		return revision;
	}
	
	private ResourceRevision updateContent(InputStream resourceStream, String resourceFileName,
			Resource resource, ResourceRevision revision) throws IOException {
		File resourceFile =  updateResourceRevisionContent(resourceStream, resource, revision);
		String checksum = getMD5Checksum(resourceFile);
		revision.setChecksum(checksum);
		resourceRevisionAccessor.save(revision);

		resource.setCurrentRevisionId(revision.getId());

		resourceAccessor.save(resource);
		return revision;
	}

	private String getMD5Checksum(File file) throws IOException {
		String hash = com.google.common.io.Files.hash(file, Hashing.md5()).toString();
		return hash;
	}

	private void updateResourceFileNameIfNecessary(String resourceFileName, Resource resource) {
		if(!resource.getResourceName().equals(resourceFileName)) {
			Map<String, String> currentAttributes = resource.getAttributes();
			if(currentAttributes == null) {
				currentAttributes = new HashMap<String, String>();
			}
			currentAttributes.put("name", resourceFileName);
			resource.setAttributes(currentAttributes);
			resource.setResourceName(resourceFileName);
			resourceAccessor.save(resource);
		}
	}

	private Resource createResource(String resourceTypeId, String name) {
		ResourceType resourceType = resourceTypes.get(resourceTypeId);
		if(resourceType ==  null) {
			throw new RuntimeException("Unknown resource type "+resourceTypeId);
		}

		Resource resource = new Resource();
		Map<String, String> attributes = new HashMap<>();
		attributes.put("name", name);
		resource.setAttributes(attributes);
		resource.setResourceName(name);
		resource.setResourceType(resourceTypeId);
		resource.setEphemeral(resourceType.isEphemeral());

		return resource;
	}

	private ResourceRevision createResourceRevision(String resourceFileName, String resourceId) {
		ResourceRevision revision = new ResourceRevision();
		revision.setResourceFileName(resourceFileName);
		revision.setResourceId(resourceId);
		return revision;
	}

	private File saveResourceRevisionContent(InputStream resourceStream, Resource resource, ResourceRevision revision) throws IOException {
		File resourceFile = getResourceRevisionFile(resource, revision);
		Files.copy(resourceStream, resourceFile.toPath());

		return resourceFile;
	}
	
	private File updateResourceRevisionContent(InputStream resourceStream, Resource resource, ResourceRevision revision) throws IOException {
		File resourceFile = getResourceRevisionFile(resource, revision);
		Files.copy(resourceStream, resourceFile.toPath(),StandardCopyOption.REPLACE_EXISTING);

		return resourceFile;
	}

	@Override
	public Resource lookupResourceByName(String resourceName) {
		Map<String, String> attributes = new HashMap<>();
		attributes.put("name", resourceName);
		return resourceAccessor.findByAttributes(attributes);
	}

	@Override
	public boolean resourceExists(String resourceId) {
		Resource resource = resourceAccessor.get(new ObjectId(resourceId));
		return resource!=null;
	}

	@Override
	public Resource saveResource(Resource resource) throws IOException {
		return resourceAccessor.save(resource);
	}
	
	@Override
	public ResourceRevision saveResourceRevision(ResourceRevision resourceRevision) throws IOException {
		return resourceRevisionAccessor.save(resourceRevision);
	}

	@Override
	public String getResourcesRootPath() {
		return resourceRootFolder.getPath();
	}
}
