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
import step.cli.AbstractDeployAutomationPackageTool;
import step.cli.StepCliExecutionException;
import step.client.credentials.ControllerCredentials;

import java.io.File;

@Mojo(name = "deploy-automation-package")
public class DeployAutomationPackageMojo extends AbstractStepPluginMojo {

    @Parameter(property = "step-deploy-automation-package.artifact-classifier")
    private String artifactClassifier;

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
            createTool(getUrl(), getStepProjectName(), getAuthToken(), getAsync(), getApVersion(), getActivationExpression()).execute();
        } catch (StepCliExecutionException e) {
            throw new MojoExecutionException("Execution exception", e);
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    protected AbstractDeployAutomationPackageTool createTool(final String url, final String projectName, final String authToken, final Boolean async,
                                                             final String apVersion, final String activationExpr) {
        return new MavenDeployAutomationPackageTool(url, projectName, authToken, async, apVersion, activationExpr);
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

    protected class MavenDeployAutomationPackageTool extends AbstractDeployAutomationPackageTool {
        public MavenDeployAutomationPackageTool(String url, String projectName, String authToken, Boolean async, String apVersion, String activationExpr) {
            super(url, projectName, authToken, async, apVersion, activationExpr);
        }

        @Override
        protected File getFileToUpload() throws StepCliExecutionException {
            try {
                return DeployAutomationPackageMojo.this.getFileToUpload();
            } catch (MojoExecutionException ex) {
                throw new StepCliExecutionException(ex);
            }
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
