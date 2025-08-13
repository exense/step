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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.client.accessors.RemoteAccessors;
import step.client.collections.remote.RemoteCollectionFactory;
import step.client.credentials.ControllerCredentials;
import step.client.resources.RemoteResourceManager;
import step.controller.multitenancy.client.MultitenancyClient;
import step.controller.multitenancy.client.RemoteMultitenancyClientImpl;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.entities.EntityManager;
import step.functions.packages.FunctionPackage;
import step.functions.packages.client.LibFileReference;
import step.functions.packages.client.RemoteFunctionPackageClientImpl;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mojo(name = "upload-keywords-package")
public class UploadKeywordsPackageMojo extends AbstractStepPluginMojo {

	@Parameter(property = "step-upload-keywords.artifact-classifier", required = false)
	private String artifactClassifier;

	@Parameter(property = "step-upload-keywords.custom-package-attrs", required = false)
	private Map<String, String> customPackageAttributes;

	@Parameter(property = "step-upload-keywords.tracking-attr", required = false)
	private String trackingAttribute;

	@Parameter(property = "step-upload-keywords.lib-step-resource-search-criteria")
	private Map<String, String> libStepResourceSearchCriteria;

	@Parameter(property = "step-upload-keywords.lib-artifact-group-id")
	private String libArtifactGroupId;

	@Parameter(property = "step-upload-keywords.lib-artifact-id")
	private String libArtifactId;

	@Parameter(property = "step-upload-keywords.lib-artifact-version")
	private String libArtifactVersion;

	@Parameter(property = "step-upload-keywords.lib-artifact-classifier", defaultValue = "")
	private String libArtifactClassifier;

	@Parameter(property = "step.step-project-name", required = false)
	private String stepProjectName;

	@Parameter(property = "step.auth-token", required = false)
	private String authToken;

	protected UploadKeywordsPackageMojo() {
	}

	@Override
	protected ControllerCredentials getControllerCredentials() {
		String authToken = getAuthToken();
		return new ControllerCredentials(getUrl(), authToken == null || authToken.isEmpty() ? null : authToken);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Uploading keywords package to Step ("+getUrl()+") ...");
		validateEEConfiguration(getStepProjectName(), getAuthToken());
		try (RemoteFunctionPackageClientImpl remoteFunctionPackageClient = createRemoteFunctionPackageClient()) {
			File packagedTarget = getFileToUpload();

			FunctionPackage previousPackage = null;
			Map<String, String> packageAttributes;

			// init with default if the key is not explicitly defined via maven parameter
			if (getCustomPackageAttributes() == null || getCustomPackageAttributes().isEmpty()) {
				packageAttributes = new HashMap<>();
			} else {
				packageAttributes = getCustomPackageAttributes();
			}

			getLog().info("Package attributes: " + packageAttributes);

			// TODO: this can be potentially replaced with some origin
			String trackingAttribute = (getTrackingAttribute() == null || getTrackingAttribute().isEmpty())
					? getProject().getGroupId() + "." + getProject().getArtifactId()
					: getTrackingAttribute();
			getLog().info("Package tracking field: " + trackingAttribute);

			// we try to find existing package (for update) if at least one tracking attribute is defined
			if (trackingAttribute != null && !trackingAttribute.isEmpty()) {
				AbstractAccessor<FunctionPackage> remoteFunctionAccessor = createRemoteFunctionPackageAccessor();

				Map<String, String> searchCriteria = new HashMap<>();
				searchCriteria.put("customFields." + FunctionPackage.TRACKING_FIELD, trackingAttribute);

				fillAdditionalPackageSearchCriteria(searchCriteria);
				getLog().info("Search for function package with tracking value: " + searchCriteria);

				previousPackage = remoteFunctionAccessor.findByCriteria(searchCriteria);
			}

			LibFileReference lib = resolveLibFile();

			FunctionPackage uploaded = null;

			if (previousPackage == null) {
				getLog().info("Uploading the new function package...");
				uploaded = remoteFunctionPackageClient.newKeywordPackageWithLibReference(lib, packagedTarget, packageAttributes, trackingAttribute);
			} else {
				getLog().info("Updating the existing function package (" + previousPackage.getId().toString() + ")...");
				uploaded = remoteFunctionPackageClient.updateKeywordPackageWithLibReference(previousPackage, lib, packagedTarget, packageAttributes, trackingAttribute);
			}
			if (uploaded == null) {
				throw new MojoExecutionException("Uploaded function package is null. Upload failed");
			}
			getLog().info("Keyword package uploaded: " + uploaded.getId().toString());
		} catch (Exception e) {
			throw logAndThrow("Unable to upload keywords package to Step", e);
		}
	}

	protected LibFileReference resolveLibFile() throws MojoExecutionException {
		Map<String, String> libStepResourceSearchCriteria = getLibStepResourceSearchCriteria();
		if (libStepResourceSearchCriteria != null && !libStepResourceSearchCriteria.isEmpty()) {
			return LibFileReference.resourceId(resolveKeywordLibResourceByCriteria(libStepResourceSearchCriteria));
		} else if (getLibArtifactId() != null && !getLibArtifactId().isEmpty()) {
			org.eclipse.aether.artifact.Artifact remoteLibArtifact = getRemoteArtifact(getLibArtifactGroupId(), getLibArtifactId(), getLibArtifactVersion(), getLibArtifactClassifier(), "jar");
			if (remoteLibArtifact == null) {
				throw new MojoExecutionException("Library artifact is not resolved");
			}
			return prepareLibraryFileReferenceForMavenArtifact(remoteLibArtifact);
		} else {
			return null;
		}
	}

	protected String resolveKeywordLibResourceByCriteria(Map<String, String> libStepResourceSearchCriteria) throws MojoExecutionException {
		getLog().info("Using Step resource " + libStepResourceSearchCriteria + " as library file");

		if (libStepResourceSearchCriteria.containsKey(ID_FIELD)) {
			// just use the specified id
			return libStepResourceSearchCriteria.get(ID_FIELD);
		} else {
			// search resources by attributes except for id
			Map<String, String> attributes = new HashMap<>(libStepResourceSearchCriteria);
			attributes.remove(ID_FIELD);
			AbstractAccessor<Resource> remoteResourcesAccessor = createRemoteResourcesAccessor();
			List<Resource> foundResources = StreamSupport.stream(remoteResourcesAccessor.findManyByAttributes(attributes), false).collect(Collectors.toList());
			if (foundResources.isEmpty()) {
				throw new MojoExecutionException("Library resource is not resolved by attributes: " + attributes);
			} else if (foundResources.size() > 1) {
				throw new MojoExecutionException("Ambiguous library resources ( " + foundResources.stream().map(AbstractIdentifiableObject::getId).collect(Collectors.toList()) + " ) are resolved by attributes: " + attributes);
			} else {
				return foundResources.get(0).getId().toString();
			}
		}
	}

	/**
	 * Tries to find the existing step resource with the specified tracking attribute and use this resource as library file for keyword package.
	 * Otherwise, uploads the remote artifact to Step and uses this just uploaded resource as library file
	 */
	protected LibFileReference prepareLibraryFileReferenceForMavenArtifact(org.eclipse.aether.artifact.Artifact remoteLibArtifact) throws MojoExecutionException {
		getLog().info("Using maven artifact " + remoteLibArtifact.getGroupId() + ":" + remoteLibArtifact.getArtifactId() + ":" + remoteLibArtifact.getVersion() + " as library file");

		try (RemoteResourceManager resourceManager = createResourceManager()) {

			String actualTrackingAttribute = artifactToString(remoteLibArtifact);

			Map<String, String> searchAttributes = new HashMap<>();
			searchAttributes.put("customFields." + Resource.TRACKING_FIELD, actualTrackingAttribute);

			getLog().info("Search for library resource with tracking value: " + searchAttributes);

			Resource previousResource = resourceManager.findManyByCriteria(searchAttributes).stream().findFirst().orElse(null);
			if (previousResource != null) {
				getLog().info("Existing library resource will be reused: " + previousResource.getId().toString());

				// for snapshot artifacts we re-upload (actualize) the resource in Step
				if (remoteLibArtifact.isSnapshot()) {
					getLog().info("Actualizing snapshot library resource " + previousResource.getId());
					try (FileInputStream is = new FileInputStream(remoteLibArtifact.getFile())) {
						// TODO: how this even works? 'saveResourceContent' is not implemented in remote resource manager
						resourceManager.saveResourceContent(previousResource.getId().toString(), is, remoteLibArtifact.getFile().getName(), null);
					} catch (IOException e) {
						throw new MojoExecutionException("Library file uploading exception", e);
					}
				}
				return LibFileReference.resourceId(previousResource.getId().toString());
			} else {
				try (FileInputStream is = new FileInputStream(remoteLibArtifact.getFile())) {
					Resource created = resourceManager.createTrackedResource(
							ResourceManager.RESOURCE_TYPE_FUNCTIONS,
							false,
							is,
							remoteLibArtifact.getFile().getName(),
							null,
							actualTrackingAttribute,
							null, null);
					getLog().info("Library resource has been created: " + created.getId().toString());
					return LibFileReference.resourceId(created.getId().toString());
				} catch (IOException e) {
					throw new MojoExecutionException("Library file uploading exception", e);
				}
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Resource manager IO Exception", e);
		}
	}

	protected AbstractAccessor<Resource> createRemoteResourcesAccessor() {
		RemoteAccessors remoteAccessors = new RemoteAccessors(new RemoteCollectionFactory(getControllerCredentials()));
		return remoteAccessors.getAbstractAccessor(EntityManager.resources, Resource.class);
	}

	protected RemoteFunctionPackageClientImpl createRemoteFunctionPackageClient() {
		RemoteFunctionPackageClientImpl client = new RemoteFunctionPackageClientImpl(getControllerCredentials());
		addProjectHeaderToRemoteClient(getStepProjectName(), client);
		return client;
	}

	protected void fillAdditionalPackageSearchCriteria(Map<String, String> searchCriteria) throws MojoExecutionException {
		if (getStepProjectName() != null && !getStepProjectName().isEmpty()) {
			getLog().info("Step project name: " + getStepProjectName());

			String projectId = new TenantHelper() {
				@Override
				protected MultitenancyClient createClient() {
					return createMultitenancyClient();
				}
			}.getProjectIdByName(getStepProjectName());

			// setup Step project and use it to search fo existing packages
			searchCriteria.put("attributes.project", projectId);
		}
	}

	protected MultitenancyClient createMultitenancyClient() {
		return new RemoteMultitenancyClientImpl(getControllerCredentials());
	}

	protected AbstractAccessor<FunctionPackage> createRemoteFunctionPackageAccessor() {
		RemoteCollectionFactory remoteCollectionFactory = new RemoteCollectionFactory(getControllerCredentials());
		addProjectHeaderToRemoteClient(getStepProjectName(), remoteCollectionFactory.getClient());

		RemoteAccessors remoteAccessors = new RemoteAccessors(remoteCollectionFactory);
		return remoteAccessors.getAbstractAccessor("functionPackage", FunctionPackage.class);
	}

	private File getFileToUpload() throws MojoExecutionException {
		Artifact artifact = getProjectArtifact(getArtifactClassifier());

		if (artifact == null || artifact.getFile() == null) {
			throw new MojoExecutionException("Unable to resolve artifact to upload.");
		}

		return artifact.getFile();
	}

	public Map<String, String> getCustomPackageAttributes() {
		return customPackageAttributes;
	}

	public void setCustomPackageAttributes(Map<String, String> customPackageAttributes) {
		this.customPackageAttributes = customPackageAttributes;
	}

	public String getTrackingAttribute() {
		return trackingAttribute;
	}

	public void setTrackingAttribute(String trackingAttribute) {
		this.trackingAttribute = trackingAttribute;
	}

	public String getArtifactClassifier() {
		return artifactClassifier;
	}

	public void setArtifactClassifier(String artifactClassifier) {
		this.artifactClassifier = artifactClassifier;
	}

	public Map<String, String> getLibStepResourceSearchCriteria() {
		return libStepResourceSearchCriteria;
	}

	public void setLibStepResourceSearchCriteria(Map<String, String> libStepResourceSearchCriteria) {
		this.libStepResourceSearchCriteria = libStepResourceSearchCriteria;
	}

	public String getLibArtifactGroupId() {
		return libArtifactGroupId;
	}

	public void setLibArtifactGroupId(String libArtifactGroupId) {
		this.libArtifactGroupId = libArtifactGroupId;
	}

	public String getLibArtifactId() {
		return libArtifactId;
	}

	public void setLibArtifactId(String libArtifactId) {
		this.libArtifactId = libArtifactId;
	}

	public String getLibArtifactVersion() {
		return libArtifactVersion;
	}

	public void setLibArtifactVersion(String libArtifactVersion) {
		this.libArtifactVersion = libArtifactVersion;
	}

	public String getLibArtifactClassifier() {
		return libArtifactClassifier;
	}

	public void setLibArtifactClassifier(String libArtifactClassifier) {
		this.libArtifactClassifier = libArtifactClassifier;
	}

	public String getStepProjectName() {
		return stepProjectName;
	}

	public void setStepProjectName(String stepProjectName) {
		this.stepProjectName = stepProjectName;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

}
