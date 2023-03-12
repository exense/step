package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
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

@Mojo(name = "upload-keywords-package")
public class UploadKeywordsPackageMojo extends AbstractStepPluginMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(property = "step-upload-keywords.custom-package-attrs", required = false)
	private Map<String, String> customPackageAttributes;

	@Parameter(property = "step-upload-keywords.step-project-id")
	private String stepProjectId;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Uploading keywords package to step...");

		try (RemoteFunctionPackageClientImpl remoteFunctionPackageClient = new RemoteFunctionPackageClientImpl(getControllerCredentials())) {
			File packagedTarget = getFileToUpload();

			FunctionPackage previousPackage = null;
			Map<String, String> packageAttributes;

			// init with default if the key is not explicitly defined via maven parameter
			if (getCustomPackageAttributes() == null || getCustomPackageAttributes().isEmpty()) {
				packageAttributes = new HashMap<>();
				packageAttributes.put("artifactId", getProject().getArtifactId());
			} else {
				packageAttributes = getCustomPackageAttributes();
			}

			getLog().info("Package attributes: " + packageAttributes);
			getLog().info("Step project id: " + getStepProjectId());

			// we try to find existing package (for update) if at least one tracking attribute is defined
			if (!packageAttributes.isEmpty()) {
				getLog().info("Trying to find existing package by attributes: " + packageAttributes);
				AbstractAccessor<FunctionPackage> remoteFunctionAccessor = getRemoteFunctionAccessor();

				Map<String, String> searchCriteria = new HashMap<>();
				for (Map.Entry<String, String> entry : packageAttributes.entrySet()) {
					searchCriteria.put("packageAttributes." + entry.getKey(), entry.getValue());
				}
				if (getStepProjectId() != null && !getStepProjectId().isBlank()) {
					searchCriteria.put("attributes.project", getStepProjectId());
				}

				previousPackage = remoteFunctionAccessor.findByCriteria(searchCriteria);
			}

			FunctionPackage uploaded = null;

			Map<String, String> customAttributes = null;
			if(getStepProjectId() != null && !getStepProjectId().isBlank()){
				customAttributes = new HashMap<>();
				customAttributes.put("project", getStepProjectId());
			}

			if (previousPackage == null) {
				getLog().info("Uploading the new function package...");
				uploaded = remoteFunctionPackageClient.newKeywordPackageWithCustomAttributes(null, packagedTarget, packageAttributes, customAttributes);
			} else {
				getLog().info("Updating the existing function package (" + previousPackage.getId().toString() + ")...");
				uploaded = remoteFunctionPackageClient.updateKeywordPackageById(previousPackage, null, packagedTarget, packageAttributes);
			}
			if (uploaded == null) {
				throw new MojoExecutionException("Uploaded function package is null. Upload failed");
			}
			getLog().info("Keyword package uploaded: " + uploaded.getId().toString());
		} catch (Exception e) {
			logAndThrow("Unable to upload keywords package to step", e);
		}
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

	public String getStepProjectId() {
		return stepProjectId;
	}

	public void setStepProjectId(String stepProjectId) {
		this.stepProjectId = stepProjectId;
	}

	private AbstractAccessor<FunctionPackage> getRemoteFunctionAccessor() {
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
