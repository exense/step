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
import com.google.common.hash.Hashing;
import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.ObjectEnricher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ResourceManagerImpl implements ResourceManager {

	private static final String ZIP_EXTENSION = ".zip";
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
		return createResourceContainer(resourceType, resourceFileName, false, null);
	}

	@Override
	public Resource createResource(String resourceType, InputStream resourceStream, String resourceFileName, boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException, InvalidResourceFormatException {
		return createResource(resourceType, false, resourceStream, resourceFileName, checkForDuplicates, objectEnricher);
	}

	@Override
	public Resource createResource(String resourceType, boolean isDirectory, InputStream resourceStream, String resourceFileName, boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException, InvalidResourceFormatException {
		ResourceRevisionContainer resourceContainer = createResourceContainer(resourceType, resourceFileName, isDirectory, null);
		FileHelper.copy(resourceStream, resourceContainer.getOutputStream(), 2048);
		resourceContainer.save(checkForDuplicates, objectEnricher);
		return resourceContainer.getResource();
	}

	@Override
	public Resource createResource(String resourceType,
								   boolean isDirectory,
								   InputStream resourceStream,
								   String resourceFileName,
								   boolean checkForDuplicates,
								   ObjectEnricher objectEnricher,
								   String trackingAttribute) throws IOException, SimilarResourceExistingException, InvalidResourceFormatException {
		ResourceRevisionContainer resourceContainer = createResourceContainer(resourceType, resourceFileName, isDirectory, trackingAttribute);
		FileHelper.copy(resourceStream, resourceContainer.getOutputStream(), 2048);
		resourceContainer.save(checkForDuplicates, objectEnricher);
		return resourceContainer.getResource();
	}

	private ResourceRevisionContainer createResourceContainer(String resourceType, String resourceFileName, boolean isDirectory, String trackingAttribute) throws IOException {
		Resource resource = createResource(resourceType, resourceFileName, isDirectory, trackingAttribute);
		ResourceRevision revision = createResourceRevisionContainer(resourceFileName, resource);
		return new ResourceRevisionContainer(resource, revision, this);
	}

	protected void closeResourceContainer(Resource resource, ResourceRevision resourceRevision, boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException, InvalidResourceFormatException {
		File resourceRevisionFile = getResourceRevisionFile(resource, resourceRevision);
		String checksum = getMD5Checksum(resourceRevisionFile);
		resourceRevision.setChecksum(checksum);

		resource.setCurrentRevisionId(resourceRevision.getId());

		if(objectEnricher != null) {
			objectEnricher.accept(resource);
		}

		resourceRevisionAccessor.save(resourceRevision);
		resourceAccessor.save(resource);

		List<Resource> resourcesWithSameChecksum = null;
		if(checkForDuplicates) {
			resourcesWithSameChecksum = getSimilarResources(resource, resourceRevision);
		}

		if(resource.isDirectory()) {
			boolean isArchive = FileHelper.isArchive(resourceRevisionFile);
			if(isArchive) {
				File revisionFileToUnzip = new File(resourceRevisionFile.getAbsolutePath() + "_beforeUnzip");
				// Rename the revision file before unzipping to avoid naming conflicts
				resourceRevisionFile.renameTo(revisionFileToUnzip);
				FileHelper.unzip(revisionFileToUnzip, resourceRevisionFile.toPath().getParent().toFile());
				revisionFileToUnzip.delete();
				resourceRevisionAccessor.save(resourceRevision);
				resourceAccessor.save(resource);
			} else {
				throw new InvalidResourceFormatException();
			}
		}

		if(checkForDuplicates && resourcesWithSameChecksum.size()>0) {
			throw new SimilarResourceExistingException(resource, resourcesWithSameChecksum);
		}
	}

	@Override
	public Resource saveResourceContent(String resourceId, InputStream resourceStream, String resourceFileName) throws IOException, InvalidResourceFormatException {
		Resource resource = getResource(resourceId);
		String resourceName = getResourceName(resourceFileName, resource.isDirectory());
		// Keep resourceName and name attribute in sync
		resource.setResourceName(resourceName);
		resource.addAttribute(AbstractOrganizableObject.NAME, resourceName);
		createResourceRevisionAndSaveContent(resourceStream, resourceFileName, resource);
		return resource;
	}

	@Override
	public void deleteResource(String resourceId) {
		Resource resource = getResource(resourceId);

		resourceRevisionAccessor.getResourceRevisionsByResourceId(resourceId).forEachRemaining(
				revision-> resourceRevisionAccessor.remove(revision.getId()));

		File resourceContainer = getResourceContainer(resource);
		if(resourceContainer.exists()) {
			FileHelper.deleteFolder(resourceContainer);
		}

		resourceAccessor.remove(resource.getId());
	}

	@Override
	public List<Resource> findManyByAttributes(Map<String, String> attributes) {
		return StreamSupport.stream(resourceAccessor.findManyByAttributes(attributes), false).collect(Collectors.toList());
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

		String resourceFileName;
		File file;
		if(resource.isDirectory()) {
			file = Files.createTempFile(resourceRevisionFile.getName(), ZIP_EXTENSION).toFile();
			FileHelper.zip(resourceRevisionFile.getParentFile(), file);
			resourceFileName = resourceRevision.getResourceFileName() + ZIP_EXTENSION;
		} else {
			file = resourceRevisionFile;
			resourceFileName = resourceRevision.getResourceFileName();
		}

		FileInputStream resourceRevisionStream = new FileInputStream(file);
		return new ResourceRevisionContentImpl(this, resource, resourceRevisionStream, resourceFileName);
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

	public ResourceRevision getResourceRevision(String resourceRevisionId) {
		return getResourceRevision(new ObjectId(resourceRevisionId));
	}

	protected File getResourceRevisionFile(Resource resource, ResourceRevision revision) {
		return new File(getResourceRevisionContainer(resource, revision).getPath()+"/"+revision.getResourceFileName());
	}

	private File getResourceRevisionContainer(Resource resource, ResourceRevision revision) {
		return new File(getResourceContainer(resource).getPath()+"/"+revision.getId().toString());
	}

	private File getResourceContainer(Resource resource) {
		File containerFile = new File(resourceRootFolder+"/"+resource.getResourceType()+"/"+resource.getId().toString());
		return containerFile;
	}

	private ResourceRevision createResourceRevisionContainer(String filename, Resource resource) throws IOException {
		ResourceRevision revision = createResourceRevision(filename, resource.getId().toString(), resource.isDirectory());
		File containerFile = getResourceRevisionContainer(resource, revision);
		boolean containerDirectoryCreated = containerFile.mkdirs();
		if(!containerDirectoryCreated) {
			throw new IOException("Unable to create container for resource "+resource.getId()+" in "+containerFile.getAbsolutePath());
		}
		return revision;
	}

	private ResourceRevision createResourceRevisionAndSaveContent(InputStream resourceStream, String contentFilename, Resource resource)
			throws IOException, InvalidResourceFormatException {
		ResourceRevision revision = createResourceRevisionContainer(contentFilename, resource);
		saveResourceRevisionContent(resourceStream, resource, revision);
		try {
			closeResourceContainer(resource, revision, false, null);
		} catch (SimilarResourceExistingException e) {
			throw new RuntimeException("Should never occur", e);
		}
		return revision;
	}

	private String getMD5Checksum(File file) throws IOException {
		return com.google.common.io.Files.hash(file, Hashing.md5()).toString();
	}

	private Resource createResource(String resourceTypeId, String name, boolean isDirectory, String trackingAttribute) {
		ResourceType resourceType = resourceTypes.get(resourceTypeId);
		if(resourceType ==  null) {
			throw new RuntimeException("Unknown resource type "+resourceTypeId);
		}

		String resourceName;
		resourceName = getResourceName(name, isDirectory);

		Resource resource = new Resource();
		resource.addAttribute(AbstractOrganizableObject.NAME, resourceName);
		resource.setResourceName(resourceName);
		resource.setResourceType(resourceTypeId);
		resource.setEphemeral(resourceType.isEphemeral());
		resource.setDirectory(isDirectory);

		if (trackingAttribute != null && !trackingAttribute.isEmpty()) {
			Map<String, String> attributes = resource.getAttributes();
			if (attributes == null) {
				attributes = new HashMap<>();
				resource.setAttributes(attributes);
			}
			attributes.put(Resource.TRACKING_FIELD, trackingAttribute);
		}

		return resource;
	}

	private String getResourceName(String contentFilename, boolean isDirectory) {
		return isDirectory ? contentFilename.replace(ZIP_EXTENSION, "") : contentFilename;
	}

	private ResourceRevision createResourceRevision(String resourceFileName, String resourceId, boolean isDirectory) {
		ResourceRevision revision = new ResourceRevision();
		revision.setResourceFileName(getResourceName(resourceFileName, isDirectory));
		revision.setResourceId(resourceId);
		return revision;
	}

	private File saveResourceRevisionContent(InputStream resourceStream, Resource resource, ResourceRevision revision) throws IOException {
		File resourceFile = getResourceRevisionFile(resource, revision);
		Files.copy(resourceStream, resourceFile.toPath());
		return resourceFile;
	}

	@Override
	public boolean resourceExists(String resourceId) {
		Resource resource = resourceAccessor.get(new ObjectId(resourceId));
		return resource!=null;
	}

	@Override
	public Resource saveResource(Resource resource) throws IOException {
		// Ensure that the name remains in sync with resourceName
		resource.addAttribute(AbstractOrganizableObject.NAME, resource.getResourceName());
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
