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
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static step.core.accessors.AbstractAccessor.ATTRIBUTES_FIELD_NAME;

public class ResourceManagerImpl implements ResourceManager {

	private static final String ZIP_EXTENSION = ".zip";
	public static final String RESOURCE_TYPE = "resourceType";
	public static final String ORIGIN = "origin";
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
		resourceTypes.put(RESOURCE_TYPE_ISOLATED_AP, new CustomResourceType(false));
		resourceTypes.put(RESOURCE_TYPE_AP, new CustomResourceType(false));
		resourceTypes.put(RESOURCE_TYPE_AP_LIBRARY, new CustomResourceType(false));
		resourceTypes.put(RESOURCE_TYPE_AP_MANAGED_LIBRARY, new CustomResourceType(false));
		resourceTypes.put(RESOURCE_TYPE_ISOLATED_AP_LIB, new CustomResourceType(false));
	}

	public void registerResourceType(String name, ResourceType resourceType) {
		resourceTypes.put(name, resourceType);
	}

	@Override
	public ResourceRevisionContainer createResourceContainer(String resourceType, String resourceFileName, String actorUser) throws IOException {
		return createResourceContainer(resourceType, resourceFileName, null, false, null, actorUser, null, null);
	}

	@Override
	public Resource createResource(String resourceType, InputStream resourceStream, String resourceFileName, ObjectEnricher objectEnricher, String actorUser) throws IOException, InvalidResourceFormatException {
		return createResource(resourceType, false, resourceStream, resourceFileName, objectEnricher, actorUser);
	}

	@Override
	public Resource createResource(String resourceType, boolean isDirectory, InputStream resourceStream, String resourceFileName, ObjectEnricher objectEnricher, String actorUser) throws IOException, InvalidResourceFormatException {
		ResourceRevisionContainer resourceContainer = createResourceContainer(resourceType, resourceFileName, null, isDirectory, null, actorUser, null, null);
		FileHelper.copy(resourceStream, resourceContainer.getOutputStream(), 2048);
		resourceContainer.save(objectEnricher);
		return resourceContainer.getResource();
	}

	@Override
	public Resource copyResource(Resource resource, ResourceManager sourceResourceManager, String actorUser) throws IOException, InvalidResourceFormatException {
		Resource resourceCopy = resource.copy(actorUser);
		File resourceFile = sourceResourceManager.getResourceFile(resource.getId().toHexString()).getResourceFile();

		ResourceRevisionContainer resourceContainer = createResourceContainer(resourceCopy);
		if(resource.isDirectory()) {
			FileHelper.zip(resourceFile.getParentFile(), resourceContainer.getOutputStream());
		} else {
			try (FileInputStream is = new FileInputStream(resourceFile)) {
				FileHelper.copy(is, resourceContainer.getOutputStream(), 2048);
			}
		}
		resourceContainer.save(null);
		return resourceContainer.getResource();
	}

	@Override
	public Resource createTrackedResource(String resourceType,
										  boolean isDirectory,
										  InputStream resourceStream,
										  String resourceFileName,
										  ObjectEnricher objectEnricher,
										  String trackingAttribute,
										  String actorUser,
										  String origin, Long originTimestamp) throws IOException, InvalidResourceFormatException {
		return createTrackedResource(resourceType, isDirectory, resourceStream, resourceFileName, null, objectEnricher, trackingAttribute, actorUser, origin, originTimestamp);
	}

	@Override
	public Resource createTrackedResource(String resourceType,
										  boolean isDirectory,
										  InputStream resourceStream,
										  String resourceFileName,
										  String optionalResourceName,
                                          ObjectEnricher objectEnricher,
										  String trackingAttribute,
										  String actorUser,
										  String origin, Long originTimestamp) throws IOException, InvalidResourceFormatException {
		ResourceRevisionContainer resourceContainer = createResourceContainer(resourceType, resourceFileName, optionalResourceName, isDirectory, trackingAttribute, actorUser, origin, originTimestamp);
		FileHelper.copy(resourceStream, resourceContainer.getOutputStream(), 2048);
		resourceContainer.save(objectEnricher);
		return resourceContainer.getResource();
	}

	private ResourceRevisionContainer createResourceContainer(Resource resource) throws IOException {
		ResourceRevision revision = createResourceRevisionContainer(resource.getResourceName(), resource);
		return new ResourceRevisionContainer(resource, revision, this);
	}

	private ResourceRevisionContainer createResourceContainer(String resourceType, String resourceFileName, String optionalResourceName, boolean isDirectory, String trackingAttribute, String actorUser, String origin, Long originTimestamp) throws IOException {
		Resource resource = createTrackedResource(resourceType, resourceFileName, optionalResourceName, isDirectory, trackingAttribute, actorUser, origin, originTimestamp);
		ResourceRevision revision = createResourceRevisionContainer(resourceFileName, resource);
		return new ResourceRevisionContainer(resource, revision, this);
	}

	protected void closeResourceContainer(Resource resource, ResourceRevision resourceRevision, ObjectEnricher objectEnricher) throws IOException, InvalidResourceFormatException {
		File resourceRevisionFile = getResourceRevisionFile(resource, resourceRevision);
		resource.setCurrentRevisionId(resourceRevision.getId());

		if(objectEnricher != null) {
			objectEnricher.accept(resource);
		}

		resourceRevisionAccessor.save(resourceRevision);
		fillPredefinedFieldsAndSave(resource);

        if(resource.isDirectory()) {
			boolean isArchive = FileHelper.isArchive(resourceRevisionFile);
			if(isArchive) {
				File revisionFileToUnzip = new File(resourceRevisionFile.getAbsolutePath() + "_beforeUnzip");
				// Rename the revision file before unzipping to avoid naming conflicts
				resourceRevisionFile.renameTo(revisionFileToUnzip);
				FileHelper.unzip(revisionFileToUnzip, resourceRevisionFile.toPath().getParent().toFile());
				revisionFileToUnzip.delete();
				resourceRevisionAccessor.save(resourceRevision);
				fillPredefinedFieldsAndSave(resource);
			} else {
				throw new InvalidResourceFormatException();
			}
		}
	}

	@Override
	public Resource saveResourceContent(String resourceId, InputStream resourceStream, String resourceFileName, String optionalResourceName, String actorUser) throws IOException, InvalidResourceFormatException {
		Resource resource = getResource(resourceId);
		String resourceName = null;

		//resource name is either the optional one provide as parameter, the origin if set or a resolution based on its file name
		if (optionalResourceName !=  null && !optionalResourceName.isBlank()) {
			resourceName = optionalResourceName;
		} else if (resource.getOrigin() != null && ! resource.getOrigin().isBlank())  {
			resourceName = resource.getOrigin();
		} else {
			resourceName = getResourceName(resourceFileName, resource.isDirectory());
		}

		// Keep resourceName and name attribute in sync
		resource.setResourceName(resourceName);
		resource.addAttribute(AbstractOrganizableObject.NAME, resourceName);
		resource.setLastModificationUser(actorUser);
		resource.setLastModificationDate(new Date());
		createResourceRevisionAndSaveContent(resourceStream, resourceFileName, resource);
		return resource;
	}

	@Override
	public void deleteResource(String resourceId) {
		deleteResourceRevisionContent(resourceId);
		resourceAccessor.remove(new ObjectId(resourceId));
	}

	@Override
	public void deleteResourceRevisionContent(String resourceId) {
		Resource resource = getResource(resourceId);

		resourceRevisionAccessor.getResourceRevisionsByResourceId(resourceId).forEachRemaining(
				revision-> resourceRevisionAccessor.remove(revision.getId()));

		File resourceContainer = getResourceContainer(resource);
		if(resourceContainer.exists()) {
			FileHelper.deleteFolder(resourceContainer);
		}
	}

	@Override
	public List<Resource> findManyByCriteria(Map<String, String> attributes) {
		return resourceAccessor.findManyByCriteria(attributes).collect(Collectors.toList());
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
		if (resource == null) {
			throw new ResourceMissingException(resourceId);
		}
		return resource;
	}

	@Override
	public Resource getResourceByNameAndType(String resourceName, String resourceType, ObjectPredicate predicate) {
		Map<String, String> criteria = new HashMap<>();
		criteria.put(ATTRIBUTES_FIELD_NAME + "." + AbstractOrganizableObject.NAME, Objects.requireNonNull(resourceName, "Name cannot be null"));
		criteria.put(RESOURCE_TYPE, Objects.requireNonNull(resourceType, "Name cannot be null"));
		return findManyByCriteria(criteria).stream().filter(predicate).findFirst().orElse(null);
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
		closeResourceContainer(resource, revision, null);

		return revision;
	}

	private Resource createTrackedResource(String resourceTypeId, String fileName, String optionalResourceName, boolean isDirectory, String trackingAttribute, String actorUser, String origin, Long originTimestamp) {
		ResourceType resourceType = resourceTypes.get(resourceTypeId);
		if(resourceType ==  null) {
			throw new RuntimeException("Unknown resource type "+resourceTypeId);
		}

		String resourceName;
		//resource name is either the optional one provide as parameter, the origin if set or a resolution based on its file name
		if (optionalResourceName !=  null && !optionalResourceName.isBlank()) {
			resourceName = optionalResourceName;
		} else if (origin != null && ! origin.isBlank())  {
			resourceName = origin;
		} else {
			resourceName = getResourceName(fileName, isDirectory);
		}

		Resource resource = new Resource(actorUser);
		// Keep resourceName and name attribute in sync
		resource.addAttribute(AbstractOrganizableObject.NAME, resourceName);
		resource.setResourceName(resourceName);
		resource.setResourceType(resourceTypeId);
		resource.setEphemeral(resourceType.isEphemeral());
		resource.setDirectory(isDirectory);

		resource.setOrigin(origin);
		resource.setOriginTimestamp(originTimestamp);

		// this TRACKING_FIELD is used to track the keyword packages
		if (trackingAttribute != null && !trackingAttribute.isEmpty()) {
			Map<String, Object> customFields = resource.getCustomFields();
			if (customFields == null) {
				customFields = new HashMap<>();
				resource.setCustomFields(customFields);
			}
			customFields.put(Resource.TRACKING_FIELD, trackingAttribute);
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
		return fillPredefinedFieldsAndSave(resource);
	}

	private Resource fillPredefinedFieldsAndSave(Resource resource) {
		// Ensure that the name remains in sync with resourceName
		resource.addAttribute(AbstractOrganizableObject.NAME, resource.getResourceName());

		// if we update the existing resource we must ensure that we don't change the creator and creation date
		ObjectId sourceId = resource.getId();
		Resource existing = (sourceId != null) ? resourceAccessor.get(sourceId) : null;
		if (existing != null) {
			resource.setCreationDate(existing.getCreationDate());
			resource.setCreationUser(existing.getCreationUser());
		}

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
