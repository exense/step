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
import step.client.accessors.RemoteAccessors;
import step.client.collections.remote.RemoteCollectionFactory;
import step.core.accessors.AbstractAccessor;
import step.functions.packages.FunctionPackage;
import step.functions.packages.client.RemoteFunctionPackageClientImpl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractUploadKeywordsPackageMojo extends AbstractStepPluginMojo {

	@Parameter(defaultValue = "${project.groupId}", readonly = true, required = true)
	private String groupId;

	@Parameter(defaultValue = "${project.artifactId}", readonly = true, required = true)
	private String artifactId;

	@Parameter(defaultValue = "${project.version}", readonly = true, required = true)
	private String artifactVersion;

	@Parameter(property = "step-upload-keywords.artifact-classifier", required = false)
	private String artifactClassifier;

	@Parameter(property = "step-upload-keywords.custom-package-attrs", required = false)
	private Map<String, String> customPackageAttributes;

	@Parameter(property = "step-upload-keywords.tracking-attr", required = false)
	private String trackingAttribute;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Uploading keywords package to Step ("+getUrl()+") ...");

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

			FunctionPackage uploaded = null;

			if (previousPackage == null) {
				getLog().info("Uploading the new function package...");
				uploaded = remoteFunctionPackageClient.newKeywordPackage(null, packagedTarget, packageAttributes, trackingAttribute);
			} else {
				getLog().info("Updating the existing function package (" + previousPackage.getId().toString() + ")...");
				uploaded = remoteFunctionPackageClient.updateKeywordPackageById(previousPackage, null, packagedTarget, packageAttributes, trackingAttribute);
			}
			if (uploaded == null) {
				throw new MojoExecutionException("Uploaded function package is null. Upload failed");
			}
			getLog().info("Keyword package uploaded: " + uploaded.getId().toString());
		} catch (Exception e) {
			throw logAndThrow("Unable to upload keywords package to Step", e);
		}
	}

	protected RemoteFunctionPackageClientImpl createRemoteFunctionPackageClient() {
		return new RemoteFunctionPackageClientImpl(getControllerCredentials());
	}

	protected void fillAdditionalPackageSearchCriteria(Map<String, String> searchCriteria) throws MojoExecutionException {

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

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getArtifactVersion() {
		return artifactVersion;
	}

	public void setArtifactVersion(String artifactVersion) {
		this.artifactVersion = artifactVersion;
	}

	protected AbstractAccessor<FunctionPackage> createRemoteFunctionPackageAccessor() {
		RemoteAccessors remoteAccessors = new RemoteAccessors(new RemoteCollectionFactory(getControllerCredentials()));
		return remoteAccessors.getAbstractAccessor("functionPackage", FunctionPackage.class);
	}

	private File getFileToUpload() throws MojoExecutionException {
		Artifact artifact = getArtifactByClassifier(getArtifactClassifier(), getGroupId(), getArtifactId(), getArtifactVersion());

		if (artifact == null || artifact.getFile() == null) {
			throw new MojoExecutionException("Unable to resolve artifact to upload.");
		}

		return artifact.getFile();
	}

}
