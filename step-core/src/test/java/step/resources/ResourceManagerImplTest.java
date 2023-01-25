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
import com.google.common.io.ByteStreams;
import org.junit.Before;
import org.junit.Test;
import step.core.accessors.AbstractOrganizableObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

import static org.junit.Assert.*;

public class ResourceManagerImplTest {

	private File rootFolder;
	private ResourceManager resourceManager;
	private ResourceAccessor resourceAccessor;
	private ResourceRevisionAccessor resourceRevisionAccessor;

	@Before
	public void before() throws IOException {
		rootFolder = FileHelper.createTempFolder();

		resourceAccessor = new InMemoryResourceAccessor();
		resourceRevisionAccessor = new InMemoryResourceRevisionAccessor();
		resourceManager = new ResourceManagerImpl(rootFolder, resourceAccessor, resourceRevisionAccessor);
	}

	@Test
	public void test() throws Exception {
		// Create a resource
		Resource resource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", false, null);
		assertNotNull(resource);
		
		// Assert that the resource has been persisted
		Resource resourceActual = resourceAccessor.get(resource.getId());
		assertEquals(resource, resourceActual);
		assertEquals("TestResource.txt", resourceActual.getResourceName());
		assertEquals("TestResource.txt", resourceActual.getAttribute(AbstractOrganizableObject.NAME));
		
		// Assert that the revision has been persisted
		ResourceRevision fisrtResourceRevisionFromDB = resourceRevisionAccessor.get(resource.getCurrentRevisionId());
		assertNotNull(fisrtResourceRevisionFromDB);
		assertEquals("TestResource.txt", fisrtResourceRevisionFromDB.getResourceFileName());
		
		String resourceId = resource.getId().toString();
		ResourceRevisionContent resourceContent = resourceManager.getResourceContent(resourceId);
		assertResourceContent(resourceContent);
		
		resourceContent = resourceManager.getResourceRevisionContent(resource.getCurrentRevisionId().toString());
		assertResourceContent(resourceContent);
		
		// Update the resource content with another name
		resourceManager.saveResourceContent(resourceId, this.getClass().getResourceAsStream("TestResource.txt"), "TestResource2.txt");
		Resource actualResource = resourceManager.getResource(resourceId);
		// Assert that the resource name matches with the new uploaded content
		assertEquals("TestResource2.txt", actualResource.getResourceName());
		assertEquals("TestResource2.txt", actualResource.getAttribute(AbstractOrganizableObject.NAME));

		ResourceRevision secondResourceRevision = resourceManager.getResourceRevision(actualResource.getCurrentRevisionId().toString());
		assertNotNull(secondResourceRevision);
		assertEquals("TestResource2.txt", secondResourceRevision.getResourceFileName());

		assertNotSame(fisrtResourceRevisionFromDB, secondResourceRevision);

		// Try to force renaming the resource
		actualResource.addAttribute(AbstractOrganizableObject.NAME, "newResourceName");
		resourceManager.saveResource(actualResource);
		// Ensure that the name of the resource remained in sync with the resourceName
		actualResource = resourceManager.getResource(resourceId);
		assertEquals("TestResource2.txt", actualResource.getAttribute(AbstractOrganizableObject.NAME));
		assertEquals("TestResource2.txt", actualResource.getResourceName());

		File resourceFileActual = new File(rootFolder.getAbsolutePath()+"/functions/"+resourceId+"/"+secondResourceRevision.getId().toString()+"/"+secondResourceRevision.getResourceFileName());
		assertTrue(resourceFileActual.exists());
		
		// Delete the resource
		resourceManager.deleteResource(resourceId);
		
		// And assert that the file and all the revisions have been deleted
		resourceFileActual = new File(rootFolder.getAbsolutePath()+"/"+secondResourceRevision.getId().toString()+"/"+secondResourceRevision.getResourceFileName());
		assertFalse(resourceFileActual.exists());
		assertFalse(resourceAccessor.getAll().hasNext());
		assertFalse(resourceRevisionAccessor.getAll().hasNext());
		
		
		// Assert that the resource doesn't exist anymore and that the correct exception is thrown
		Exception actualException = null;
		try {
			resourceManager.saveResourceContent(resourceId, null, null);			
		} catch (Exception e) {
			actualException = e;
		}
		assertNotNull(actualException);
		assertEquals("The resource with ID "+resourceId+" doesn't exist", actualException.getMessage());
	}

	@Test
	public void testResourceContainer() throws Exception {
		ResourceRevisionContainer resourceContainer = resourceManager.createResourceContainer(ResourceManager.RESOURCE_TYPE_FUNCTIONS, "TestResource.txt");
		ByteStreams.copy(this.getClass().getResourceAsStream("TestResource.txt"), resourceContainer.getOutputStream());
		resourceContainer.save(null);

		Resource actualResource = resourceManager.getResource(resourceContainer.getResource().getId().toString());
		assertNotNull(actualResource);

		assertEquals("TestResource.txt", actualResource.getAttribute(AbstractOrganizableObject.NAME));
		assertEquals("TestResource.txt", actualResource.getResourceName());
	}

	@Test
	public void testDuplicateResource() throws Exception {
		// Create a resource
		resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", true, null);
		resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, this.getClass().getResourceAsStream("TestResource2.txt"), "TestResource2.txt", true, null);
		resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", false, null);
		SimilarResourceExistingException actualException = null;
		try {
			resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", true, null);
		} catch (SimilarResourceExistingException e) {
			actualException = e;
		}
		assertNotNull(actualException);
		assertEquals(2, actualException.getSimilarResources().size());
	}
	
	@Test
	public void testDeletedResourceException() throws Exception {
		// Create a resource
		Resource resource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", true, null);
		File resourceFileActual = new File(rootFolder.getAbsolutePath()+"/"+ResourceManager.RESOURCE_TYPE_FUNCTIONS+"/"+resource.getId().toString()+"/"+resource.getCurrentRevisionId().toString()+"/TestResource.txt");
		resourceFileActual.delete();
		
		Exception actualException = null;
		try {
			resourceManager.getResourceContent(resource.getId().toString());
		} catch (Exception e) {
			actualException = e;
		}
		assertNotNull(actualException);
		assertEquals("The resource revision file " +resourceFileActual.getAbsolutePath() +" doesn't exist or cannot be read", actualException.getMessage());
	}
	
	@Test
	public void testEphemeralResources() throws Exception {
		// Create a resource
		Resource resource = resourceManager.createResource("temp", this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", true, null);
		
		String resourceId = resource.getId().toString();
		ResourceRevisionContent resourceContent = resourceManager.getResourceContent(resourceId);
		resourceContent.close();
		
		Exception actualException = null;
		try {
			resourceManager.getResourceContent(resourceId);
		} catch (Exception e) {
			actualException = e;
		}
		assertNotNull(actualException);
		assertEquals("The resource with ID "+resourceId+" doesn't exist" , actualException.getMessage());
	}
	
	@Test
	public void testEphemeralResources2() throws Exception {
		// Create a resource
		Resource resource = resourceManager.createResource("temp", this.getClass().getResourceAsStream("TestResource.txt"), "TestResource.txt", true, null);
		
		String resourceId = resource.getId().toString();
		ResourceRevisionFileHandle resourceFile = resourceManager.getResourceFile(resourceId);
		resourceFile.close();
		
		Exception actualException = null;
		try {
			resourceManager.getResourceContent(resourceId);
		} catch (Exception e) {
			actualException = e;
		}
		assertNotNull(actualException);
		assertEquals("The resource with ID "+resourceId+" doesn't exist" , actualException.getMessage());
	}

	protected void assertResourceContent(ResourceRevisionContent resourceContent) throws IOException {
		assertNotNull(resourceContent);
		assertEquals("TestResource.txt", resourceContent.getResourceName());
		
		// Assert that the content is the correct one
		String text;
	    try (Scanner scanner = new Scanner(resourceContent.getResourceStream(), StandardCharsets.UTF_8.name())) {
	        text = scanner.useDelimiter("\\A").next();
	    }
		assertEquals("TEST", text);
		
		resourceContent.close();
	}

	@Test
	public void testDirectoryResource() throws Exception {
		// Create a folder
		File tempFolder = FileHelper.createTempFolder();
		tempFolder.toPath().resolve("TestResource").toFile().mkdir();
		tempFolder.toPath().resolve("TestResource/file1").toFile().createNewFile();
		tempFolder.toPath().resolve("TestResource/file2").toFile().createNewFile();
		File zippedFolder = FileHelper.createTempFile();

		// Zip the folder
		FileHelper.zip(tempFolder, zippedFolder);

		// Create a directory resource
		Resource resource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, true, new FileInputStream(zippedFolder), "TestResource.zip", true, null);
		assertEquals("TestResource", resource.getAttribute(AbstractOrganizableObject.NAME));
		assertEquals("TestResource", resource.getResourceName());

		// Assert that the resource has been created
		String resourceId = resource.getId().toString();
		assertTrue(resourceManager.resourceExists(resourceId));

		// Assert that the resource can be retrieved and is the same as the one returned at creation
		Resource actualResource = resourceManager.getResource(resourceId);
		assertEquals(resource, actualResource);
		assertEquals(resource.getResourceName(), actualResource.getResourceName());
		assertEquals(resource.getCurrentRevisionId(), actualResource.getCurrentRevisionId());

		// Assert that the resource has been extracted and saved as directory
		ResourceRevisionFileHandle resourceFile = resourceManager.getResourceFile(resourceId);
		assertTrue(resourceFile.getResourceFile().isDirectory());

		// Get the content and assert that it has been returned as Zip
		ResourceRevisionContent resourceContent = resourceManager.getResourceContent(resourceId);
		File resourceContentFile = FileHelper.createTempFile();
		Files.copy(resourceContent.getResourceStream(), resourceContentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		assertTrue(FileHelper.isArchive(resourceContentFile));

		ResourceRevision resourceRevision = resourceManager.getResourceRevision(actualResource.getCurrentRevisionId().toString());
		assertEquals("TestResource", resourceRevision.getResourceFileName());

		Resource updatedResource = resourceManager.saveResourceContent(resourceId, new FileInputStream(resourceContentFile), "newName.zip");
		// Assert that the name of the resource and the resourceName have been updated accordingly
		assertEquals("newName", updatedResource.getAttribute(AbstractOrganizableObject.NAME));
		assertEquals("newName", updatedResource.getResourceName());

		resourceRevision = resourceManager.getResourceRevision(updatedResource.getCurrentRevisionId().toString());
		assertEquals("newName", resourceRevision.getResourceFileName());
	}

}
