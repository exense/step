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

    @Parameter(defaultValue = "${project.groupId}", readonly = true, required = true)
    private String groupId;

    @Parameter(defaultValue = "${project.artifactId}", readonly = true, required = true)
    private String artifactId;

    @Parameter(defaultValue = "${project.version}", readonly = true, required = true)
    private String artifactVersion;

    @Parameter(property = "step-deploy-automation-package.artifact-classifier")
    private String artifactClassifier;

    @Parameter(property = "step.step-project-name")
    private String stepProjectName;

    @Parameter(property = "step.auth-token")
    private String authToken;

    @Parameter(property = "step-deploy-automation-package.async")
    private Boolean async;

    @Override
    protected ControllerCredentials getControllerCredentials() {
        String authToken = getAuthToken();
        return new ControllerCredentials(getUrl(), authToken == null || authToken.isEmpty() ? null : authToken);
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            createTool(getUrl(), getStepProjectName(), getAuthToken(), getAsync()).execute();
        } catch (StepCliExecutionException e) {
            throw new MojoExecutionException("Execution exception", e);
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    protected AbstractDeployAutomationPackageTool createTool(final String url, final String projectName, final String authToken, final Boolean async) {
        return new MavenDeployAutomationPackageTool(url, projectName, authToken, async);
    }

    protected File getFileToUpload() throws MojoExecutionException {
        Artifact artifact = getProjectArtifact(getArtifactClassifier(), getGroupId(), getArtifactId(), getArtifactVersion());

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

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
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

    protected class MavenDeployAutomationPackageTool extends AbstractDeployAutomationPackageTool {
        public MavenDeployAutomationPackageTool(String url, String projectName, String authToken, Boolean async) {
            super(url, projectName, authToken, async);
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
        protected void logError(String errorText, Throwable e) {
            if (e != null) {
                DeployAutomationPackageMojo.this.getLog().error(errorText, e);
            } else {
                DeployAutomationPackageMojo.this.getLog().error(errorText);
            }
        }

        @Override
        protected void logInfo(String infoText, Throwable e) {
            if (e != null) {
                DeployAutomationPackageMojo.this.getLog().info(infoText, e);
            } else {
                DeployAutomationPackageMojo.this.getLog().info(infoText);
            }
        }
    }
}
