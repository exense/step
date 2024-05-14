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
import step.client.AbstractRemoteClient;
import step.client.accessors.RemoteAccessors;
import step.client.collections.remote.RemoteCollectionFactory;
import step.client.credentials.ControllerCredentials;
import step.client.resources.RemoteResourceManager;
import step.controller.multitenancy.Constants;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.entities.EntityManager;
import step.functions.packages.client.LibFileReference;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractStepPluginMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(property = "step.url", required = true)
	private String url;

	@Parameter(defaultValue = "${project.build.finalName}", readonly = true)
	private String buildFinalName;

	@Parameter(defaultValue = "${project.version}", readonly = true)
	private String projectVersion;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	protected MavenSession session;

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

	protected AbstractAccessor<Resource> createRemoteResourcesAccessor() {
		RemoteAccessors remoteAccessors = new RemoteAccessors(new RemoteCollectionFactory(getControllerCredentials()));
		return remoteAccessors.getAbstractAccessor(EntityManager.resources, Resource.class);
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

	protected Artifact getProjectArtifact(String artifactClassifier, String groupId, String artifactId, String artifactVersion) {
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
						resourceManager.saveResourceContent(previousResource.getId().toString(), is, remoteLibArtifact.getFile().getName());
					} catch (IOException e) {
						throw new MojoExecutionException("Library file uploading exception", e);
					}
				}
				return LibFileReference.resourceId(previousResource.getId().toString());
			} else {
				try (FileInputStream is = new FileInputStream(remoteLibArtifact.getFile())) {
					Resource created = resourceManager.createResource(
							ResourceManager.RESOURCE_TYPE_FUNCTIONS,
							false,
							is,
							remoteLibArtifact.getFile().getName(),
							false, null,
							actualTrackingAttribute
					);
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
