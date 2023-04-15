package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import step.client.resources.RemoteResourceManager;
import step.core.repositories.RepositoryObjectReference;
import step.resources.Resource;
import step.resources.SimilarResourceExistingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public abstract class AbstractRunPackagedAutomationPackagesMojo extends AbstractRunAutomationPackagesMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// 1. Upload the packaged artifact as resource to Step
		String resourceId = uploadResourceToStep();
		if (resourceId == null) {
			throw logAndThrow("Unable to upload automation package to Step", new MojoExecutionException("Unable to run automation package"));
		}
		Map<String, Object> executionContext = new HashMap<>();
		executionContext.put("resourceId", resourceId);

		// 2. Execute just uploaded artifact in Step
		executeBundleOnStep(executionContext);
	}

	protected String uploadResourceToStep() throws MojoExecutionException {
		try (RemoteResourceManager resourceManager = createRemoteResourceManager()) {
			File fileToUpload = getFileToUpload();
			if(fileToUpload == null){
				throw logAndThrow("Unable to detect an artifact to upload", getDefaultMojoException());
			} else {
				getLog().info("Artifact is detected for upload to Step: " + fileToUpload.getName());

				Resource uploaded = resourceManager.createResource("temp", new FileInputStream(fileToUpload), fileToUpload.getName(), false, null);
				if(uploaded == null){
					throw logAndThrow("Uploaded resource is null", getDefaultMojoException());
				} else {
					getLog().info("Artifact has been uploaded as resource to Step: " + uploaded.getId());
					return uploaded.getId().toString();
				}
			}
		} catch (IOException | SimilarResourceExistingException e) {
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
		return new RepositoryObjectReference("ResourceArtifact", prepareRepositoryParameters((String) executionContext.get("resourceId")));
	}

	private HashMap<String, String> prepareRepositoryParameters(String resourceId) {
		HashMap<String, String> repoParams = new HashMap<>();
		repoParams.put("resourceId", resourceId);
		return repoParams;
	}

	private File getFileToUpload() {
		Artifact applicableArtifact = getArtifactByClassifier(getArtifactClassifier(), getGroupId(), getArtifactId(), getArtifactVersion());

		if (applicableArtifact != null) {
			return applicableArtifact.getFile();
		} else {
			return null;
		}
	}

}
