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
import step.client.credentials.ControllerCredentials;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;

@Mojo(name = "deploy-automation-package")
public class DeployAutomationPackageMojo extends AbstractStepPluginMojo {

    @Parameter(property = "step.step-project-name")
    private String stepProjectName;

    @Parameter(property = "step.auth-token")
    private String authToken;

    @Parameter(property = "step-deploy-automation-package.async")
    private Boolean async;

    @Parameter(property = "step-deploy-automation-package.ap-version")
    private String apVersion;

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

    @Parameter(property = "step-deploy-auto-packages.lib-artifact-path")
    private String libArtifactPath;
    @Parameter(property = "step-deploy-auto-packages.lib-artifact-group-id")
    private String libArtifactGroupId;
    @Parameter(property = "step-deploy-auto-packages.lib-artifact-id")
    private String libArtifactId;
    @Parameter(property = "step-deploy-auto-packages.lib-artifact-version")
    private String libArtifactVersion;
    @Parameter(property = "step-deploy-auto-packages.lib-artifact-classifier", required = false)
    private String libArtifactClassifier;
    @Parameter(property = "step-deploy-auto-packages.lib-artifact-type", required = false)
    private String libArtifactType;

    @Parameter(property = "step-deploy-auto-packages.force-upload", required = false)
    private Boolean allowUpdateOfOtherPackages;

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
            createTool(getUrl(), getStepProjectName(), getAuthToken(), getAsync(), getApVersion(), getActivationExpression(), getLibArtifactPath(), getallowUpdateOfOtherPackages()).execute();
        } catch (StepCliExecutionException e) {
            throw new MojoExecutionException("Execution exception", e);
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    protected DeployAutomationPackageTool createTool(final String url, final String projectName, final String authToken, final Boolean async,
                                                     final String apVersion, final String activationExpr, String libArtifactPath, Boolean allowUpdateOfOtherPackages) throws MojoExecutionException {
        MavenArtifactIdentifier remoteApMavenIdentifier = getRemoteMavenIdentifier();
        File localApFile = remoteApMavenIdentifier != null ? null : DeployAutomationPackageMojo.this.getFileToUpload();
        return new MavenDeployAutomationPackageTool(
                url, new DeployAutomationPackageTool.Params()
                .setAutomationPackageMavenArtifact(remoteApMavenIdentifier)
                .setAutomationPackageFile(localApFile)
                .setPackageLibraryFile(libArtifactPath == null ? null : new File(libArtifactPath))
                .setPackageLibraryMavenArtifact(getKeywordLibRemoteMavenIdentifier())
                .setStepProjectName(projectName)
                .setAuthToken(authToken)
                .setAsync(async)
                .setallowUpdateOfOtherPackages(allowUpdateOfOtherPackages)
                .setApVersion(apVersion)
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

    public String getStepProjectName() {
        return stepProjectName;
    }

    public void setStepProjectName(String stepProjectName) {
        this.stepProjectName = stepProjectName;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public String getApVersion() {
        return apVersion;
    }

    public void setApVersion(String apVersion) {
        this.apVersion = apVersion;
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

    public String getLibArtifactGroupId() {
        return libArtifactGroupId;
    }

    public void setLibArtifactGroupId(String libArtifactGroupId) {
        this.libArtifactGroupId = libArtifactGroupId;
    }

    public String getLibArtifactId() {
        return libArtifactId;
    }

    public void setLibArtifactId(String libArtifactId) {
        this.libArtifactId = libArtifactId;
    }

    public String getLibArtifactVersion() {
        return libArtifactVersion;
    }

    public void setLibArtifactVersion(String libArtifactVersion) {
        this.libArtifactVersion = libArtifactVersion;
    }

    public String getLibArtifactClassifier() {
        return libArtifactClassifier;
    }

    public void setLibArtifactClassifier(String libArtifactClassifier) {
        this.libArtifactClassifier = libArtifactClassifier;
    }

    public String getLibArtifactType() {
        return libArtifactType;
    }

    public void setLibArtifactType(String libArtifactType) {
        this.libArtifactType = libArtifactType;
    }

    public String getLibArtifactPath() {
        return libArtifactPath;
    }

    public void setLibArtifactPath(String libArtifactPath) {
        this.libArtifactPath = libArtifactPath;
    }

    public Boolean getallowUpdateOfOtherPackages() {
        return allowUpdateOfOtherPackages;
    }

    public void setallowUpdateOfOtherPackages(Boolean allowUpdateOfOtherPackages) {
        this.allowUpdateOfOtherPackages = allowUpdateOfOtherPackages;
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

    protected MavenArtifactIdentifier getKeywordLibRemoteMavenIdentifier() throws MojoExecutionException {
        if(getLibArtifactId() != null && !getLibArtifactId().isEmpty() && getLibArtifactGroupId() != null && !getLibArtifactGroupId().isEmpty()){
            return new MavenArtifactIdentifier(getLibArtifactGroupId(), getLibArtifactId(), getLibArtifactVersion(), getLibArtifactClassifier(), getLibArtifactType());
        } else {
            return null;
        }
    }

    protected class MavenDeployAutomationPackageTool extends DeployAutomationPackageTool {

        public MavenDeployAutomationPackageTool(String url, Params params) {
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
