package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import step.client.accessors.RemoteAccessors;
import step.client.collections.remote.RemoteCollectionFactory;
import step.core.accessors.AbstractAccessor;
import step.functions.packages.FunctionPackage;
import step.functions.packages.client.RemoteFunctionPackageClientImpl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractUploadKeywordsPackageMojo extends AbstractStepPluginMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(property = "step-upload-keywords.custom-package-attrs", required = false)
	private Map<String, String> customPackageAttributes;

	@Parameter(property = "step-upload-keywords.tracking-attr", required = false)
	private String trackingAttribute;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Uploading keywords package to Step...");

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

	public MavenProject getProject() {
		return project;
	}

	public void setProject(MavenProject project) {
		this.project = project;
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

	protected AbstractAccessor<FunctionPackage> createRemoteFunctionPackageAccessor() {
		RemoteAccessors remoteAccessors = new RemoteAccessors(new RemoteCollectionFactory(getControllerCredentials()));
		return remoteAccessors.getAbstractAccessor("functionPackage", FunctionPackage.class);
	}

	private File getFileToUpload() throws MojoExecutionException {
		Artifact artifact = project.getArtifact();

		if (artifact == null || artifact.getFile() == null) {
			throw new MojoExecutionException("Unable to resolve artifact to deploy.");
		}

		getLog().info("Resolved artifact: "
				+ artifact.getGroupId() + ":"
				+ artifact.getArtifactId() + ":"
				+ artifact.getClassifier() + ":"
				+ artifact.getVersion()
		);

		return artifact.getFile();
	}

}
