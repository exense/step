package step.resources;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.Test;

import junit.framework.Assert;
import step.commons.helpers.FileHelper;

public class ResourceManagerImplTest {

	@Test
	public void test() throws IOException, SimilarResourceExistingException {
		File rootFolder = FileHelper.createTempFolder();
		
		ResourceAccessor resourceAccessor = new InMemoryResourceAccessor();
		ResourceRevisionAccessor resourceRevisionAccessor = new InMemoryResourceRevisionAccessor();
		ResourceManager resourceManager = new ResourceManagerImpl(rootFolder, resourceAccessor, resourceRevisionAccessor);
		
		// Create a resource
		Resource resource = resourceManager.createResource("test", this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", false);
		Assert.assertNotNull(resource);
		
		// Assert that the resource has been persisted
		Resource resourceActual = resourceAccessor.get(resource.getId());
		Assert.assertEquals(resource, resourceActual);
		
		// Assert that the revision has been persisted
		ResourceRevision fisrtResourceRevisionFromDB = resourceRevisionAccessor.get(resource.getCurrentRevisionId());
		Assert.assertNotNull(fisrtResourceRevisionFromDB);
		Assert.assertEquals("TestResource.txt", fisrtResourceRevisionFromDB.getResourceFileName());
		
		String resourceId = resource.getId().toString();
		ResourceRevisionContent resourceContent = resourceManager.getResourceContent(resourceId);
		assertResourceContent(resourceContent);
		
		resourceContent = resourceManager.getResourceRevisionContent(resource.getCurrentRevisionId().toString());
		assertResourceContent(resourceContent);
		
		// Update the resource content
		resourceManager.saveResourceContent(resourceId, this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt");
		ResourceRevision secondResourceRevision = resourceManager.getResourceRevisionByResourceId(resourceId);
		Assert.assertNotNull(secondResourceRevision);
		
		ResourceRevision secondResourceRevisionFromResource = resourceManager.getResourceRevisionByResourceId(resourceId);
		Assert.assertEquals(secondResourceRevisionFromResource, secondResourceRevision);
		Assert.assertNotSame(fisrtResourceRevisionFromDB, secondResourceRevision);
		
		File resourceFileActual = new File(rootFolder.getAbsolutePath()+"/test/"+resourceId+"/"+secondResourceRevisionFromResource.getId().toString()+"/"+secondResourceRevisionFromResource.getResourceFileName());
		Assert.assertTrue(resourceFileActual.exists());
		
		
		// Delete the resource
		resourceManager.deleteResource(resourceId);
		
		// And assert that the file and all the revisions have been deleted
		resourceFileActual = new File(rootFolder.getAbsolutePath()+"/"+secondResourceRevisionFromResource.getId().toString()+"/"+secondResourceRevisionFromResource.getResourceFileName());
		Assert.assertFalse(resourceFileActual.exists());
		Assert.assertEquals(false, resourceAccessor.getAll().hasNext());
		Assert.assertEquals(false, resourceRevisionAccessor.getAll().hasNext());
		
		
		// Assert that the resource doesn't exist anymore and that the correct exception is thrown
		Exception actualException = null;
		try {
			resourceManager.saveResourceContent(resourceId, null, null);			
		} catch (Exception e) {
			actualException = e;
		}
		Assert.assertNotNull(actualException);
		Assert.assertEquals("The resource with ID "+resourceId+" doesn't exist", actualException.getMessage());
	}
	
	@Test
	public void testDuplicateResource() throws IOException, SimilarResourceExistingException {
		File rootFolder = FileHelper.createTempFolder();
		
		ResourceAccessor resourceAccessor = new InMemoryResourceAccessor();
		ResourceRevisionAccessor resourceRevisionAccessor = new InMemoryResourceRevisionAccessor();
		ResourceManager resourceManager = new ResourceManagerImpl(rootFolder, resourceAccessor, resourceRevisionAccessor);
		
		// Create a resource
		resourceManager.createResource("test", this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", true);
		resourceManager.createResource("test", this.getClass().getResourceAsStream("TestResource2.txt"), "TestResource2.txt", true);
		resourceManager.createResource("test", this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", false);
		SimilarResourceExistingException actualException = null;
		try {
			resourceManager.createResource("test", this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", true);
		} catch (SimilarResourceExistingException e) {
			actualException = e;
		}
		Assert.assertNotNull(actualException);
		Assert.assertEquals(2, actualException.getSimilarResources().size());
		
	}
	
	@Test
	public void testDeletedResourceException() throws IOException, SimilarResourceExistingException {
		File rootFolder = FileHelper.createTempFolder();
		
		ResourceAccessor resourceAccessor = new InMemoryResourceAccessor();
		ResourceRevisionAccessor resourceRevisionAccessor = new InMemoryResourceRevisionAccessor();
		ResourceManager resourceManager = new ResourceManagerImpl(rootFolder, resourceAccessor, resourceRevisionAccessor);
		
		// Create a resource
		Resource resource = resourceManager.createResource("test", this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", true);
		File resourceFileActual = new File(rootFolder.getAbsolutePath()+"/test/"+resource.getId().toString()+"/"+resource.getCurrentRevisionId().toString()+"/TestResource.txt");
		resourceFileActual.delete();
		
		
		Exception actualException = null;
		try {
			resourceManager.getResourceContent(resource.getId().toString());
		} catch (Exception e) {
			actualException = e;
		}
		Assert.assertNotNull(actualException);
		Assert.assertEquals("The resource revision file " +resourceFileActual.getAbsolutePath() +" doesn't exist or cannot be read", actualException.getMessage());
	}
	
	@Test
	public void testEphemeralResources() throws IOException, SimilarResourceExistingException {
		File rootFolder = FileHelper.createTempFolder();
		
		ResourceAccessor resourceAccessor = new InMemoryResourceAccessor();
		ResourceRevisionAccessor resourceRevisionAccessor = new InMemoryResourceRevisionAccessor();
		ResourceManager resourceManager = new ResourceManagerImpl(rootFolder, resourceAccessor, resourceRevisionAccessor);
		
		// Create a resource
		Resource resource = resourceManager.createResource("temp", this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", true);
		
		String resourceId = resource.getId().toString();
		ResourceRevisionContent resourceContent = resourceManager.getResourceContent(resourceId);
		resourceContent.close();
		
		Exception actualException = null;
		try {
			resourceManager.getResourceContent(resourceId);
		} catch (Exception e) {
			actualException = e;
		}
		Assert.assertNotNull(actualException);
		Assert.assertEquals("The resource with ID "+resourceId+" doesn't exist" , actualException.getMessage());
	}
	
	@Test
	public void testEphemeralResources2() throws IOException, SimilarResourceExistingException {
		File rootFolder = FileHelper.createTempFolder();
		
		ResourceAccessor resourceAccessor = new InMemoryResourceAccessor();
		ResourceRevisionAccessor resourceRevisionAccessor = new InMemoryResourceRevisionAccessor();
		ResourceManager resourceManager = new ResourceManagerImpl(rootFolder, resourceAccessor, resourceRevisionAccessor);
		
		// Create a resource
		Resource resource = resourceManager.createResource("temp", this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", true);
		
		String resourceId = resource.getId().toString();
		ResourceRevisionFileHandle resourceFile = resourceManager.getResourceFile(resourceId);
		resourceFile.close();
		
		Exception actualException = null;
		try {
			resourceManager.getResourceContent(resourceId);
		} catch (Exception e) {
			actualException = e;
		}
		Assert.assertNotNull(actualException);
		Assert.assertEquals("The resource with ID "+resourceId+" doesn't exist" , actualException.getMessage());
	}

	protected void assertResourceContent(ResourceRevisionContent resourceContent) throws IOException {
		Assert.assertNotNull(resourceContent);
		Assert.assertEquals("TestResource.txt", resourceContent.getResourceName());
		
		// Assert that the content is the correct one
		String text = null;
	    try (Scanner scanner = new Scanner(resourceContent.getResourceStream(), StandardCharsets.UTF_8.name())) {
	        text = scanner.useDelimiter("\\A").next();
	    }
		Assert.assertEquals("TEST", text);
		
		resourceContent.close();
	}

}
