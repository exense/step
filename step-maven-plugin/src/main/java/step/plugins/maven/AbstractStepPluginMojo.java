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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import step.cli.ControllerVersionValidator;
import step.cli.StepCliExecutionException;
import step.client.AbstractRemoteClient;
import step.client.controller.ControllerServicesClient;
import step.client.credentials.ControllerCredentials;
import step.client.resources.RemoteResourceManager;
import step.core.Constants;
import step.core.Version;
import step.core.accessors.AbstractIdentifiableObject;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractStepPluginMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;

	@Parameter(property = "step.url", required = true)
	private String url;

	@Parameter(defaultValue = "${project.build.finalName}", readonly = true)
	private String buildFinalName;

	@Parameter(defaultValue = "${project.version}", readonly = true)
	private String projectVersion;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	protected MavenSession session;

	@Parameter(defaultValue = "false")
	protected Boolean force;

	@Component
	protected RepositorySystem repositorySystem;

	protected static final String ID_FIELD = AbstractIdentifiableObject.ID;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getBuildFinalName() {
		return buildFinalName;
	}

	public void setBuildFinalName(String buildFinalName) {
		this.buildFinalName = buildFinalName;
	}

	public String getProjectVersion() {
		return projectVersion;
	}

	public void setProjectVersion(String projectVersion) {
		this.projectVersion = projectVersion;
	}

	public MavenProject getProject() {
		return project;
	}

	public void setProject(MavenProject project) {
		this.project = project;
	}

	public Boolean getForce() {
		return force;
	}

	public void setForce(Boolean force) {
		this.force = force;
	}

	protected MojoExecutionException logAndThrow(String errorText, Throwable e) {
		getLog().error(errorText, e);
		return new MojoExecutionException(errorText, e);
	}

	protected MojoExecutionException logAndThrow(String errorText) {
		getLog().error(errorText);
		return new MojoExecutionException(errorText);
	}

	protected ControllerCredentials getControllerCredentials() {
		return new ControllerCredentials(getUrl(), null);
	}

	protected ControllerServicesClient createControllerServicesClient(){
		return new ControllerServicesClient(getControllerCredentials());
	}

	protected void checkStepControllerVersion() throws MojoExecutionException {
		try {
			new ControllerVersionValidator(createControllerServicesClient()).validateVersions(getStepVersion());
		} catch (ControllerVersionValidator.ValidationException e) {
			String msg = "Version mismatch. The server version " + e.getResult().getServerVersion() + " is incompatible with the current maven plugin version " + e.getResult().getClientVersion() + ". Please ensure both the CLI and server are running compatible versions.";
			if (e.getResult().getStatus() == ControllerVersionValidator.Status.MINOR_MISMATCH) {
				getLog().warn(msg);
			} else {
				if (!force) {
					msg += " You can use the \"force\" parameter to ignore this validation.";
					throw new MojoExecutionException(msg, e);
				} else {
					getLog().warn(msg);
				}
			}
		}
	}

	protected Version getStepVersion() {
		return Constants.STEP_API_VERSION;
	}

	protected org.eclipse.aether.artifact.Artifact getRemoteArtifact(String groupId, String artifactId, String artifactVersion, String classifier, String extension) throws MojoExecutionException {
		ArtifactResult artifactResult;
		try {
			List<RemoteRepository> repositories = getProject().getRemoteProjectRepositories();
			artifactResult = repositorySystem.resolveArtifact(
					session.getRepositorySession(),
					new ArtifactRequest(new DefaultArtifact(groupId, artifactId, classifier, extension, artifactVersion), repositories, null)
			);
		} catch (ArtifactResolutionException e) {
			throw logAndThrow("unable to resolve artefact", e);
		}

		if (artifactResult != null) {
			return artifactResult.getArtifact();
		} else {
			return null;
		}
	}

	protected Artifact getProjectArtifact(String artifactClassifier) {
		Set<Artifact> allProjectArtifacts = new HashSet<>(getProject().getArtifacts());
		allProjectArtifacts.add(getProject().getArtifact());
		allProjectArtifacts.addAll(getProject().getAttachedArtifacts());
		Artifact applicableArtifact = null;

		List<String> artifactStrings = allProjectArtifacts.stream().map(this::artifactToString).collect(Collectors.toList());
		getLog().info("All detected project artifacts: " + artifactStrings);

		for (Artifact a : allProjectArtifacts) {
			if (Objects.equals(a.getGroupId(), project.getGroupId()) && Objects.equals(a.getArtifactId(), project.getArtifactId()) && Objects.equals(a.getVersion(), project.getVersion())) {
				if (artifactClassifier != null && !artifactClassifier.isEmpty()) {
					if (Objects.equals(a.getClassifier(), artifactClassifier)) {
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
			getLog().info("Resolved artifact: " + artifactToString(applicableArtifact));
		}

		return applicableArtifact;
	}

	protected RemoteResourceManager createResourceManager() {
		return new RemoteResourceManager(getControllerCredentials());
	}

	protected String artifactToString(Artifact artifact) {
		return artifactToString(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getVersion());
	}

	protected String artifactToString(org.eclipse.aether.artifact.Artifact artifact) {
		return artifactToString(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getVersion());
	}

	protected String artifactToString(String groupId, String artifactId, String classifier, String version){
		String s = groupId + ":" + artifactId;
		if (classifier != null && !classifier.isEmpty()) {
			s = s + ":" + classifier;
		}
		s = s + ":" + version;
		return s;
	}

	protected void addProjectHeaderToRemoteClient(String stepProjectName, AbstractRemoteClient remoteClient) {
		if (stepProjectName != null && !stepProjectName.isEmpty()) {
			remoteClient.getHeaders().addProjectName(stepProjectName);
		}
	}

}
