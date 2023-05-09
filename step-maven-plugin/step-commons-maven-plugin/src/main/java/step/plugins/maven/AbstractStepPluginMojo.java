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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import step.client.credentials.ControllerCredentials;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractStepPluginMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(property = "step.url", required = true)
	private String url;

	@Parameter(defaultValue = "${project.build.finalName}", readonly = true)
	private String buildFinalName;

	@Parameter(defaultValue = "${project.version}", readonly = true)
	private String projectVersion;

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

	protected MojoExecutionException logAndThrow(String errorText, Throwable e) {
		getLog().error(errorText, e);
		return new MojoExecutionException(errorText, e);
	}

	protected ControllerCredentials getControllerCredentials() {
		return new ControllerCredentials(getUrl(), null);
	}

	protected Artifact getArtifactByClassifier(String artifactClassifier, String groupId, String artifactId, String artifactVersion) {
		Set<Artifact> allProjectArtifacts = new HashSet<>(getProject().getArtifacts());
		allProjectArtifacts.add(getProject().getArtifact());
		allProjectArtifacts.addAll(getProject().getAttachedArtifacts());
		Artifact applicableArtifact = null;

		List<String> artifactStrings = allProjectArtifacts.stream().map(this::artifactToString).collect(Collectors.toList());
		getLog().info("All detected project artifacts: " + artifactStrings);

		for (Artifact a : allProjectArtifacts) {
			if (Objects.equals(a.getGroupId(), groupId) && Objects.equals(a.getArtifactId(), artifactId) && Objects.equals(a.getVersion(), artifactVersion)) {
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

	private String artifactToString(Artifact artifact) {
		String s = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
		if (artifact.getClassifier() != null) {
			s = s + ":" + artifact.getClassifier();
		}
		return s;
	}
}
