package step.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;

import step.commons.helpers.FileHelper;

public class ResourceManagerImpl implements ResourceManager {

	protected final File resourceRootFolder;
	protected final ResourceAccessor resourceAccessor;
	protected final ResourceRevisionAccessor resourceRevisionAccessor;
	
	public ResourceManagerImpl(File resourceRootFolder, ResourceAccessor resourceAccessor,
			ResourceRevisionAccessor resourceRevisionAccessor) {
		super();
		this.resourceRootFolder = resourceRootFolder;
		this.resourceAccessor = resourceAccessor;
		this.resourceRevisionAccessor = resourceRevisionAccessor;
	}
	
	@Override
	public Resource createResource(String resourceType, InputStream resourceStream, String resourceFileName, boolean checkForDuplicates) throws IOException, SimilarResourceExistingException {
		Resource resource = createResource(resourceType, resourceFileName);
		ResourceRevision resourceRevision = createResourceRevisionAndSaveContent(resourceStream, resourceFileName, resource);
		
		if(checkForDuplicates) {
			List<Resource> resourcesWithSameChecksum = getSimilarResources(resource, resourceRevision);
			if(resourcesWithSameChecksum.size()>0) {
				throw new SimilarResourceExistingException(resource, resourcesWithSameChecksum);
			}
		}
		
		return resource;
	}
	
	@Override
	public Resource saveResourceContent(String resourceId, InputStream resourceStream, String resourceFileName) throws IOException {
		Resource resource = getResource(resourceId);
		createResourceRevisionAndSaveContent(resourceStream, resourceFileName, resource);
		updateResourceFileNameIfNecessary(resourceFileName, resource);
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
				if(resource.getCurrentRevisionId().equals(revision.getId())) {
					try {
						if(FileUtils.contentEquals(getResourceRevisionFile(resource, revision), getResourceRevisionFile(actualResource, actualResourceRevision))) {
							result.add(resource);
						}
					} catch (IOException e) {

					}
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
	
	@Override
	public ResourceRevision getResourceRevisionByResourceId(String resourceId) {
		Resource resource = getResource(resourceId);
		ResourceRevision resourceRevision = getCurrentResourceRevision(resource);
		return resourceRevision;
	}

	@Override
	public ResourceRevisionContent getResourceRevisionContent(String resourceRevisionId) throws IOException {
		ResourceRevision resourceRevision = getResourceRevision(new ObjectId(resourceRevisionId));
		Resource resource = getResource(resourceRevision.getResourceId());
		return getResourceRevisionContent(resource, resourceRevision);
	}
	
	@Override
	public File getResourceFile(String resourceId) {
		Resource resource = getResource(resourceId);
		ResourceRevision resourceRevision = getCurrentResourceRevision(resource);
		return getResourceRevisionFile(resource, resourceRevision);
	}
	
	private ResourceRevisionContent getResourceRevisionContent(Resource resource, ResourceRevision resourceRevision)
			throws IOException {
		File resourceRevisionFile = getResourceRevisionFile(resource, resourceRevision);
		if(!resourceRevisionFile.exists() || !resourceRevisionFile.canRead()) {
			throw new IOException("The resource revision file "+resourceRevisionFile.getAbsolutePath()+" doesn't exist or cannot be read");
		}
		FileInputStream resourceRevisionStream = new FileInputStream(resourceRevisionFile);
		return new ResourceRevisionContent(resourceRevisionStream, resourceRevision.getResourceFileName());
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

	private Resource getResource(String resourceId) {
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

	private String getMD5Checksum(File file) throws IOException {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Unable to find MD5 algorithm", e);
		}
		try (InputStream is = Files.newInputStream(file.toPath());
		     DigestInputStream dis = new DigestInputStream(is, md)) 
		{}
		byte[] digest = md.digest();
		
		String result = "";

		for (int i = 0; i < digest.length; i++) {
			result += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}
	
	private void updateResourceFileNameIfNecessary(String resourceFileName, Resource resource) {
		if(!resource.getResourceName().equals(resourceFileName)) {
			resource.setResourceName(resourceFileName);
			resourceAccessor.save(resource);
		}
	}
	
	private Resource createResource(String resourceType, String name) {
		Resource resource = new Resource();
		Map<String, String> attributes = new HashMap<>();
		attributes.put(Resource.NAME, name);
		resource.setAttributes(attributes);
		resource.setResourceName(name);
		resource.setResourceType(resourceType);
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
}
