package step.resources;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceManager {

	public static final String RESOURCE_TYPE_PDF_TEST_SCENARIO_FILE = "pdfTestScenarioFile";
	public static final String RESOURCE_TYPE_SECRET = "secret";
	public static final String RESOURCE_TYPE_DATASOURCE = "datasource";
	public static final String RESOURCE_TYPE_FUNCTIONS = "functions";
	public static final String RESOURCE_TYPE_STAGING_CONTEXT_FILES = "stagingContextFiles";
	public static final String RESOURCE_TYPE_ATTACHMENT = "attachment";
	public static final String RESOURCE_TYPE_TEMP = "temp";
	
	/**
	 * Create a new resource and save the content provided as stream under a new {@link ResourceRevision}
	 * 
	 * @param resourceType the type of the resource
	 * @param resourceStream the stream of the resource to be saved
	 * @param resourceFileName the name of the resource (filename)
	 * @param checkForDuplicates is duplicate should be checked
	 * @return the created {@link Resource}
	 * @throws IOException an IOException occurs during the call
	 * @throws SimilarResourceExistingException a similar resource exist
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
	 * @throws IOException an IOException occurs during the call
	 */
	Resource saveResourceContent(String resourceId, InputStream resourceStream, String resourceFileName)
			throws IOException;

	/**
	 * Test if a given resource id exists
	 * 
	 * @param resourceId the id of the resource to test
	 * @return true if the resource exists 
	 */
	boolean resourceExists(String resourceId);
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
	 * @throws IOException an IOException occurs during the call
	 */
	ResourceRevisionContent getResourceContent(String resourceId) throws IOException;
	
	ResourceRevisionFileHandle getResourceFile(String resourceId);

	ResourceRevision getResourceRevisionByResourceId(String resourceId);

	ResourceRevisionContentImpl getResourceRevisionContent(String resourceRevisionId) throws IOException;

	ResourceRevisionContainer createResourceContainer(String resourceType, String resourceFileName) throws IOException;

	Resource lookupResourceByName(String resourcename);
}