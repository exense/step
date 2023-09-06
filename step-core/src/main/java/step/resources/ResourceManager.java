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

import step.core.objectenricher.ObjectEnricher;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface ResourceManager {

	String RESOURCE_TYPE_PDF_TEST_SCENARIO_FILE = "pdfTestScenarioFile";
	String RESOURCE_TYPE_SECRET = "secret";
	String RESOURCE_TYPE_DATASOURCE = "datasource";
	String RESOURCE_TYPE_FUNCTIONS = "functions";
	String RESOURCE_TYPE_STAGING_CONTEXT_FILES = "stagingContextFiles";
	String RESOURCE_TYPE_ATTACHMENT = "attachment";
	String RESOURCE_TYPE_TEMP = "temp";
	
	/**
	 * @param resourceType the type of the resource
	 * @param resourceStream the stream of the resource to be saved
	 * @param resourceFileName the name of the resource (filename)
	 * @param checkForDuplicates is duplicate should be checked
	 * @param objectEnricher the {@link ObjectEnricher} of the context
	 * @return the created {@link Resource} 
	 * @throws IOException an IOException occurs during the call
	 * @throws SimilarResourceExistingException a similar resource exist
	 */
	Resource createResource(String resourceType, InputStream resourceStream, String resourceFileName, boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException, InvalidResourceFormatException;

	Resource createResource(String resourceType, boolean isDirectory, InputStream resourceStream, String resourceFileName, boolean checkForDuplicates, ObjectEnricher objectEnricher) throws IOException, SimilarResourceExistingException, InvalidResourceFormatException;

	ResourceRevisionContainer createResourceContainer(String resourceType, String resourceFileName) throws IOException;

	/**
	 * Test if a given resource id exists
	 *
	 * @param resourceId the id of the resource to test
	 * @return true if the resource exists
	 */
	boolean resourceExists(String resourceId);

	/**
	 * Get the content of an existing {@link Resource}
	 * @param resourceId the id of the {@link Resource} to be deleted
	 * @return the content of the resource as stream
	 * @throws IOException an IOException occurs during the call
	 */
	ResourceRevisionContent getResourceContent(String resourceId) throws IOException;

	ResourceRevisionFileHandle getResourceFile(String resourceId);

	Resource getResource(String resourceId);

	ResourceRevisionContentImpl getResourceRevisionContent(String resourceRevisionId) throws IOException;

	ResourceRevision getResourceRevision(String resourceRevisionId);

	String getResourcesRootPath();

	Resource createResource(String resourceType,
							boolean isDirectory,
							InputStream resourceStream,
							String resourceFileName,
							boolean checkForDuplicates,
							ObjectEnricher objectEnricher,
							String trackingAttribute) throws IOException, SimilarResourceExistingException, InvalidResourceFormatException;

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
			throws IOException, InvalidResourceFormatException;

	/**
	 * Saved the resource object only
	 * @param resource the resource to be saved
	 * @return the updated {@link Resource}
	 * @throws IOException an IOException occurs during the call
	 */
	Resource saveResource(Resource resource) throws IOException;

	ResourceRevision saveResourceRevision(ResourceRevision resourceRevision) throws IOException;

	/**
	 * Delete the resource and all its revisions 
	 * 
	 * @param resourceId the id of the {@link Resource} to be deleted
	 */
	void deleteResource(String resourceId);

	List<Resource> findManyByAttributes(Map<String, String> criteria);

}
