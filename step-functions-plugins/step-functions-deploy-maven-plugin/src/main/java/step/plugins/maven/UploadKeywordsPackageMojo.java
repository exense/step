package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import step.functions.packages.FunctionPackage;
import step.functions.packages.client.RemoteFunctionPackageClientImpl;

import java.io.File;
import java.util.HashMap;

@Mojo(name = "upload-keywords-package")
public class UploadKeywordsPackageMojo extends AbstractStepPluginMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Uploading keywords package to step...");


		try (RemoteFunctionPackageClientImpl remoteFunctionPackageClient = new RemoteFunctionPackageClientImpl(getControllerCredentials())) {
			// TODO: package library file, package attributes?
			File packagedTarget = getFileToUpload();

			FunctionPackage uploaded = remoteFunctionPackageClient.newKeywordPackage(null, packagedTarget, new HashMap<>());
			if (uploaded == null) {
				throw new MojoExecutionException("Uploaded function package is null. Upload failed");
			}
			getLog().info("Keyword package uploaded: " + uploaded.getId().toString());
		} catch (Exception e) {
			logAndThrow("Unable to upload keywords package to step", e);
		}
	}


	private File getFileToUpload() throws MojoExecutionException {
		Artifact artifact = project.getArtifact();

		if(artifact == null || artifact.getFile() == null){
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
