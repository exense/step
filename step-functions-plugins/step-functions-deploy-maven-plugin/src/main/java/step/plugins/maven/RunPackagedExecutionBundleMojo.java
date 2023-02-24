package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import step.client.resources.RemoteResourceManager;
import step.core.repositories.RepositoryObjectReference;
import step.resources.Resource;
import step.resources.SimilarResourceExistingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "run-packaged-execution-bundle")
public class RunPackagedExecutionBundleMojo extends AbstractRunExecutionBundleMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// 1. Upload the packaged artifact as resource to step
		String resourceId = uploadResourceToStep();
		if (resourceId == null) {
			logAndThrow("Unable to upload execution bundle to step", new MojoExecutionException("Unable to run execution bundle"));
		}
		Map<String, Object> executionContext = new HashMap<>();
		executionContext.put("resourceId", resourceId);

		// 2. Execute just uploaded artifact in step
		executeBundleOnStep(executionContext);
	}

	protected String uploadResourceToStep() throws MojoExecutionException {
		try (RemoteResourceManager resourceManager = new RemoteResourceManager(getControllerCredentials())) {
			File fileToUpload = getFileToUpload();
			if(fileToUpload == null){
				logAndThrow("Unable to detect an artifact to upload", getDefaultMojoException());
			} else {
				getLog().info("Artifact is detected for upload to step: " + fileToUpload.getName());
				// TODO: new resource type = 'artifact'?
				Resource uploaded = resourceManager.createResource("temp", new FileInputStream(fileToUpload), fileToUpload.getName(), false, null);
				if(uploaded == null){
					logAndThrow("Uploaded resource is null", getDefaultMojoException());
				} else {
					getLog().info("Artifact has been uploaded as resource to step: " + uploaded.getId());
					return uploaded.getId().toString();
				}
			}
		} catch (IOException | SimilarResourceExistingException e) {
			logAndThrow("Unable to upload packaged resource to step", e);
		}
		return null;
	}

	private static MojoExecutionException getDefaultMojoException() {
		return new MojoExecutionException("Unable to upload package resource to step");
	}

	@Override
	protected RepositoryObjectReference prepareExecutionRepositoryObject(Map<String, Object> executionContext) {
		return new RepositoryObjectReference("ResourceArtifact", prepareRepositoryParameters((String) executionContext.get("resourceId")));
	}

	private HashMap<String, String> prepareRepositoryParameters(String resourceId) {
		HashMap<String, String> repoParams = new HashMap<>();
		repoParams.put("artifactId", resourceId);
		return repoParams;
	}

	private File getFileToUpload() {
		Set<Artifact> allProjectArtifacts = new HashSet<>(project.getArtifacts());
		allProjectArtifacts.add(project.getArtifact());
		allProjectArtifacts.addAll(project.getAttachedArtifacts());
		Artifact applicableArtifact = null;

		List<String> artifactStrings = allProjectArtifacts.stream().map(this::artifactToString).collect(Collectors.toList());
		getLog().info("All detected project artifacts: " + artifactStrings);

		for (Artifact a : allProjectArtifacts) {
			if (Objects.equals(a.getGroupId(), getGroupId()) && Objects.equals(a.getArtifactId(), getArtifactId()) && Objects.equals(a.getVersion(), getArtifactVersion())) {
				if (getArtifactClassifier() != null) {
					if (Objects.equals(a.getClassifier(), getArtifactClassifier())) {
						applicableArtifact = a;
					}
				} else if (a.getClassifier() == null || a.getClassifier().equals("jar")) {
					applicableArtifact = a;
				}
			}
			if (applicableArtifact != null) {
				break;
			}
		}

		if (applicableArtifact != null) {
			return applicableArtifact.getFile();
		} else {
			return null;
		}
	}

	private String artifactToString(Artifact artifact) {
		return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() + ":" + artifact.getClassifier();
	}
}
