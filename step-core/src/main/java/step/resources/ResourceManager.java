package step.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public interface ResourceManager {

	/**
	 * Create a new resource and save the content provided as stream under a new {@link ResourceRevision}
	 * 
	 * @param resourceStream the stream of the resource to be saved
	 * @param resourceFileName the name of the resource (filename)
	 * @return the created {@link Resource}
	 * @throws IOException
	 * @throws SimilarResourceExistingException 
	 */
	Resource createResource(String resourceType, InputStream resourceStream, String resourceFileName, boolean checkForDuplicates) throws IOException, SimilarResourceExistingException;

	/**
	 * Save the content provided as stream to an existing resource. 
	 * This creates a new {@link ResourceRevision} for the {@link Resource}
	 * and saves the content provided as stream under this revision.
	 * 
	 * @param resourceId the id of the resource to be updated
	 * @param resourceStream the stream of the resource to be saved
	 * @param resourceFileName the name of the resource (filename) 
	 * @return the updated {@link Resource}
	 * @throws IOException
	 */
	Resource saveResourceContent(String resourceId, InputStream resourceStream, String resourceFileName)
			throws IOException;

	/**
	 * Delete the resource and all its revisions 
	 * 
	 * @param resourceId the id of the {@link Resource} to be deleted
	 */
	void deleteResource(String resourceId);

	/**
	 * Get the content of an existing {@link Resource}
	 * @param resourceId the id of the {@link Resource} to be deleted
	 * @return the content of the resource as stream
	 * @throws FileNotFoundException
	 */
	ResourceRevisionContent getResourceContent(String resourceId) throws IOException;
	
	ResourceRevisionFileHandle getResourceFile(String resourceId);

	ResourceRevision getResourceRevisionByResourceId(String resourceId);

	ResourceRevisionContentImpl getResourceRevisionContent(String resourceRevisionId) throws IOException;

	ResourceRevisionContainer createResourceContainer(String resourceType, String resourceFileName) throws IOException;

}