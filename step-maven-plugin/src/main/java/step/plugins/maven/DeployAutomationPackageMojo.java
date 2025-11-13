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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.cli.DeployAutomationPackageTool;
import step.cli.StepCliExecutionException;
import step.cli.parameters.ApDeployParameters;
import step.client.credentials.ControllerCredentials;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;

@Mojo(name = "deploy-automation-package")
public class DeployAutomationPackageMojo extends AbstractStepPluginMojo {


    @Parameter(property = "step-deploy-automation-package.async")
    private Boolean async;
    @Parameter(property = "step-deploy-automation-package.version-name")
    private String versionName;
    @Parameter(property = "step-deploy-automation-package.activation-expression")
    private String activationExpression;
    @Parameter(property = "step-deploy-auto-packages.artifact-group-id")
    private String artifactGroupId;
    @Parameter(property = "step-deploy-auto-packages.artifact-id")
    private String artifactId;
    @Parameter(property = "step-deploy-auto-packages.artifact-version")
    private String artifactVersion;
    @Parameter(property = "step-deploy-auto-packages.artifact-classifier", required = false)
    private String artifactClassifier;
    @Parameter(property = "step-deploy-auto-packages.artifact-type", required = false)
    private String artifactType;

    @Parameter(property = "step-deploy-auto-packages.library")
    private LibraryConfiguration library;

    @Parameter(property = "step-deploy-auto-packages.force-refresh-snapshots", required = false)
    private Boolean forceRefreshOfSnapshots;

    @Override
    protected ControllerCredentials getControllerCredentials() {
        String authToken = getAuthToken();
        return new ControllerCredentials(getUrl(), authToken == null || authToken.isEmpty() ? null : authToken);
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            validateEEConfiguration(getStepProjectName(), getAuthToken());
            checkStepControllerVersion();
            if (library != null) {
                library.validate();
            }
            createTool(getUrl(), getStepProjectName(), getAuthToken(), getAsync(), getVersionName(), getActivationExpression(), getForceRefreshOfSnapshots()).execute();
        } catch (StepCliExecutionException e) {
            throw new MojoExecutionException("Execution exception", e);
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    protected DeployAutomationPackageTool createTool(final String url, final String projectName, final String authToken, final Boolean async,
                                                     final String apVersion, final String activationExpr, Boolean forceRefreshOfSnapshots) throws MojoExecutionException {
        MavenArtifactIdentifier remoteApMavenIdentifier = getRemoteMavenIdentifier();
        File localApFile = remoteApMavenIdentifier != null ? null : DeployAutomationPackageMojo.this.getFileToUpload();

        File libraryFile = library != null ? library.toFile() : null;
        MavenArtifactIdentifier libraryMavenArtifact = library != null ? library.toMavenArtifactIdentifier() : null;
        String libraryName = library != null && library.isManagedLibraryNameConfigured() ? library.getManaged() : null;

        return new MavenDeployAutomationPackageTool(
                url, new ApDeployParameters()
                .setAutomationPackageMavenArtifact(remoteApMavenIdentifier)
                .setAutomationPackageFile(localApFile)
                .setLibraryFile(libraryFile)
                .setlibraryMavenArtifact(libraryMavenArtifact)
                .setManagedLibraryName(libraryName)
                .setStepProjectName(projectName)
                .setAuthToken(authToken)
                .setAsync(async)
                .setForceRefreshOfSnapshots(forceRefreshOfSnapshots)
                .setVersionName(apVersion)
                .setActivationExpression(activationExpr)
        );
    }

    protected File getFileToUpload() throws MojoExecutionException {
        Artifact artifact = getProjectArtifact(getArtifactClassifier());

        if (artifact == null || artifact.getFile() == null) {
            throw new MojoExecutionException("Unable to resolve artifact to upload.");
        }

        return artifact.getFile();
    }

    public String getArtifactClassifier() {
        return artifactClassifier;
    }

    public void setArtifactClassifier(String artifactClassifier) {
        this.artifactClassifier = artifactClassifier;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getActivationExpression() {
        return activationExpression;
    }

    public void setActivationExpression(String activationExpression) {
        this.activationExpression = activationExpression;
    }

    public String getArtifactGroupId() {
        return artifactGroupId;
    }

    public void setArtifactGroupId(String artifactGroupId) {
        this.artifactGroupId = artifactGroupId;
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

    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    public LibraryConfiguration getLibrary() {
        return library;
    }

    public void setLibrary(LibraryConfiguration library) {
        this.library = library;
    }

    public Boolean getForceRefreshOfSnapshots() {
        return forceRefreshOfSnapshots;
    }

    public void setForceRefreshOfSnapshots(Boolean forceRefreshOfSnapshots) {
        this.forceRefreshOfSnapshots = forceRefreshOfSnapshots;
    }

    protected boolean isLocalMavenArtifact() {
        return getArtifactId() == null || getArtifactId().isEmpty() || getArtifactGroupId() == null || getArtifactGroupId().isEmpty();
    }

    /**
     * @return the identifier of the remote maven artifact (alternatively to the deployment of current maven artefact)
     */
    protected MavenArtifactIdentifier getRemoteMavenIdentifier() throws MojoExecutionException {
        MavenArtifactIdentifier remoteMavenArtifact = null;
        if (!isLocalMavenArtifact()) {
            remoteMavenArtifact = new MavenArtifactIdentifier(getArtifactGroupId(), getArtifactId(), getArtifactVersion(), getArtifactClassifier(), getArtifactType());
        }
        return remoteMavenArtifact;
    }

    protected class MavenDeployAutomationPackageTool extends DeployAutomationPackageTool {

        public MavenDeployAutomationPackageTool(String url, ApDeployParameters params) {
            super(url, params);
        }

        @Override
        public void logError(String errorText, Throwable e) {
            if (e != null) {
                DeployAutomationPackageMojo.this.getLog().error(errorText, e);
            } else {
                DeployAutomationPackageMojo.this.getLog().error(errorText);
            }
        }

        @Override
        public void logInfo(String infoText, Throwable e) {
            if (e != null) {
                DeployAutomationPackageMojo.this.getLog().info(infoText, e);
            } else {
                DeployAutomationPackageMojo.this.getLog().info(infoText);
            }
        }
    }
}
