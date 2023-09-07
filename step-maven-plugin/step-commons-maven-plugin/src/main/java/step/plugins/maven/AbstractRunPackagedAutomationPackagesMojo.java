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
package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import step.client.resources.RemoteResourceManager;
import step.core.repositories.RepositoryObjectReference;
import step.repositories.ArtifactRepositoryConstants;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.SimilarResourceExistingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractRunPackagedAutomationPackagesMojo extends AbstractRunAutomationPackagesMojo {

	@Parameter(property = "step-run-auto-packages.lib-step-resource-search-criteria")
	private Map<String, String> libStepResourceSearchCriteria;

	protected static final String PLUGIN_CTX_RESOURCE_ID = "resourceId";
	protected static final String PLUGIN_CTX_LIB_RESOURCE_ID = "libResourceId";

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// 1. Upload the packaged artifact as resource to Step
		String resourceId = null;
		try {
			resourceId = uploadResourceToStep(getFileToUpload(), false);
		} catch (SimilarResourceExistingException e) {
			throw logAndThrow("Unable to upload automation package. Similar resources detected", e);
		}

		if (resourceId == null) {
			throw logAndThrow("Unable to upload automation package to Step", new MojoExecutionException("Unable to run automation package"));
		}
		Map<String, Object> executionContext = new HashMap<>();
		executionContext.put(PLUGIN_CTX_RESOURCE_ID, resourceId);

		// 2. Resolve additional library if required
		String libResourceId = resolveAndUploadLibraryResource();
		if (libResourceId != null) {
			// add lib resource id
			executionContext.put(PLUGIN_CTX_LIB_RESOURCE_ID, libResourceId);
		}

		// 3. Execute just uploaded artifact in Step
		executeBundleOnStep(executionContext);
	}

	protected String resolveAndUploadLibraryResource() throws MojoExecutionException {
		// for packaged automation packages we support 2 ways to define library file: by step resource id and as maven artifact
		String libResourceId = null;
		Map<String, String> libStepResourceSearchCriteria = getLibStepResourceSearchCriteria();
		if (libStepResourceSearchCriteria != null && !libStepResourceSearchCriteria.isEmpty()) {
			// reference library via resource id explicitly
			libResourceId = resolveKeywordLibResourceByCriteria(libStepResourceSearchCriteria);
		} else if (getLibArtifactId() != null && !getLibArtifactId().isEmpty()) {
			getLog().info("Using maven artifact " + getLibArtifactGroupId() + ":" + getLibArtifactId() + ":" + getLibArtifactVersion() + " as library file");

			// use deployed artifact as resource
			org.eclipse.aether.artifact.Artifact remoteLibArtifact = getRemoteArtifact(getLibArtifactGroupId(), getLibArtifactId(), getLibArtifactVersion(), getLibArtifactClassifier(), "jar");
			if (remoteLibArtifact == null) {
				throw new MojoExecutionException("Library artifact is not resolved");
			}

			try {
				// checkForDuplicates = true to restrict uploading the same library twice
				libResourceId = uploadResourceToStep(remoteLibArtifact.getFile(), true);
			} catch (SimilarResourceExistingException e) {
				if (e.getSimilarResources() != null && !e.getSimilarResources().isEmpty()) {
					getLog().info("Existing similar library resource is detected and will be reused as library file");
					libResourceId = e.getSimilarResources().get(0).getId().toString();
				}
			}
		}
		if (libResourceId != null) {
			getLog().info("The following Step resource will be used as keyword library: " + libResourceId);
		}
		return libResourceId;
	}

	protected String uploadResourceToStep(File fileToUpload, boolean checkForDuplicates) throws MojoExecutionException, SimilarResourceExistingException {
		try (RemoteResourceManager resourceManager = createRemoteResourceManager()) {
			if(fileToUpload == null){
				throw logAndThrow("Unable to detect an artifact to upload", getDefaultMojoException());
			} else {
				getLog().info("Artifact is detected for upload to Step: " + fileToUpload.getName());

				Resource uploaded = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_FUNCTIONS, new FileInputStream(fileToUpload), fileToUpload.getName(), checkForDuplicates, null);
				if(uploaded == null){
					throw logAndThrow("Uploaded resource is null", getDefaultMojoException());
				} else {
					getLog().info("Artifact has been uploaded as resource to Step: " + uploaded.getId());
					return uploaded.getId().toString();
				}
			}
		} catch (IOException e) {
			throw logAndThrow("Unable to upload packaged resource to Step", e);
		}
	}

	protected RemoteResourceManager createRemoteResourceManager() {
		return new RemoteResourceManager(getControllerCredentials());
	}

	private static MojoExecutionException getDefaultMojoException() {
		return new MojoExecutionException("Unable to upload package resource to Step");
	}

	@Override
	protected RepositoryObjectReference prepareExecutionRepositoryObject(Map<String, Object> executionContext) {
		return new RepositoryObjectReference(
				ArtifactRepositoryConstants.RESOURCE_REPO_ID,
				prepareRepositoryParameters((String) executionContext.get(PLUGIN_CTX_RESOURCE_ID), (String) executionContext.get(PLUGIN_CTX_LIB_RESOURCE_ID))
		);
	}

	private HashMap<String, String> prepareRepositoryParameters(String resourceId, String libResourceId) {
		HashMap<String, String> repoParams = new HashMap<>();
		repoParams.put(ArtifactRepositoryConstants.RESOURCE_PARAM_RESOURCE_ID, resourceId);
		if (libResourceId != null) {
			repoParams.put(ArtifactRepositoryConstants.RESOURCE_PARAM_LIB_RESOURCE_ID, libResourceId);
		}
		return repoParams;
	}

	private File getFileToUpload() {
		Artifact applicableArtifact = getProjectArtifact(getArtifactClassifier(), getGroupId(), getArtifactId(), getArtifactVersion());

		if (applicableArtifact != null) {
			return applicableArtifact.getFile();
		} else {
			return null;
		}
	}

	public Map<String, String> getLibStepResourceSearchCriteria() {
		return libStepResourceSearchCriteria;
	}

	public void setLibStepResourceSearchCriteria(Map<String, String> libStepResourceSearchCriteria) {
		this.libStepResourceSearchCriteria = libStepResourceSearchCriteria;
	}
}
