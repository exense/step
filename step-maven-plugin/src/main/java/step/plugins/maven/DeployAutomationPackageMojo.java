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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.automation.packages.AutomationPackageUpdateResult;
import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
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
    public void execute() throws MojoExecutionException, MojoFailureException {
        try (RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClient()) {
            File packagedTarget = getFileToUpload();

            getLog().info("Uploading the automation package...");
            try {
                AutomationPackageUpdateResult updateResult = automationPackageClient.createOrUpdateAutomationPackage(packagedTarget, async != null && async);
                if (updateResult != null && updateResult.getId() != null) {
                    getLog().info("Automation package successfully uploaded. With status " + updateResult.getStatus() + ". Id: " + updateResult.getId());
                } else {
                    throw logAndThrow("Unexpected response from Step. The returned automation package id is null. Please check the controller logs.");
                }
            } catch (AutomationPackageClientException e) {
                throw logAndThrow("Error while uploading automation package to Step: " + e.getMessage());
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    protected File getFileToUpload() throws MojoExecutionException {
        Artifact artifact = getProjectArtifact(getArtifactClassifier(), getGroupId(), getArtifactId(), getArtifactVersion());

        if (artifact == null || artifact.getFile() == null) {
            throw new MojoExecutionException("Unable to resolve artifact to upload.");
        }

        return artifact.getFile();
    }

    protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
        RemoteAutomationPackageClientImpl client = new RemoteAutomationPackageClientImpl(getControllerCredentials());
        addProjectHeaderToRemoteClient(getStepProjectName(), client);
        return client;
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
}
