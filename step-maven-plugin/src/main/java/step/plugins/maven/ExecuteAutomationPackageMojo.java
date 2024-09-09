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
import step.cli.AbstractExecuteAutomationPackageTool;
import step.cli.StepCliExecutionException;

import java.io.File;
import java.util.Map;

@Mojo(name = "execute-automation-package")
public class ExecuteAutomationPackageMojo extends AbstractStepPluginMojo {

    @Parameter(property = "step.step-project-name", required = false)
    private String stepProjectName;
    @Parameter(property = "step-execute-auto-packages.user-id", required = false)
    private String userId;
    @Parameter(property = "step.auth-token", required = false)
    private String authToken;

    @Parameter(property = "step-execute-auto-packages.group-id", required = true, defaultValue = "${project.groupId}")
    private String groupId;
    @Parameter(property = "step-execute-auto-packages.artifact-id", required = true, defaultValue = "${project.artifactId}")
    private String artifactId;
    @Parameter(property = "step-execute-auto-packages.artifact-version", required = true, defaultValue = "${project.version}")
    private String artifactVersion;
    @Parameter(property = "step-execute-auto-packages.artifact-classifier", required = false)
    private String artifactClassifier;

    @Parameter(property = "step-execute-auto-packages.execution-parameters", required = false)
    private Map<String, String> executionParameters;
    @Parameter(property = "step-execute-auto-packages.exec-result-timeout-s", defaultValue = "3600")
    private Integer executionResultTimeoutS;
    @Parameter(property = "step-execute-auto-packages.wait-for-exec", defaultValue = "true")
    private Boolean waitForExecution;
    @Parameter(property = "step-execute-auto-packages.ensure-exec-success", defaultValue = "true")
    private Boolean ensureExecutionSuccess;

    @Parameter(property = "step-execute-auto-packages.include-plans")
    private String includePlans;
    @Parameter(property = "step-execute-auto-packages.exclude-plans")
    private String excludePlans;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            createTool(getUrl(), getStepProjectName(), getUserId(), getAuthToken(), getExecutionParameters(), getExecutionResultTimeoutS(), getWaitForExecution(), getEnsureExecutionSuccess(), getIncludePlans(), getExcludePlans()).execute();
        } catch (StepCliExecutionException e) {
            throw new MojoExecutionException("Execution exception", e);
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    protected AbstractExecuteAutomationPackageTool createTool(final String url, final String projectName, final String userId, final String authToken, final Map<String, String> parameters, final Integer executionResultTimeoutS, final Boolean waitForExecution, final Boolean ensureExecutionSuccess, final String includePlans, final String excludePlans) {
        return new AbstractExecuteAutomationPackageTool(url, projectName, userId, authToken, parameters, executionResultTimeoutS, waitForExecution, ensureExecutionSuccess, includePlans, excludePlans, groupId, artifactId, artifactVersion, artifactClassifier) {
            @Override
            protected File getAutomationPackageFile() throws StepCliExecutionException {
                Artifact applicableArtifact = getProjectArtifact(getArtifactClassifier(), getGroupId(), getArtifactId(), getArtifactVersion());

                if (applicableArtifact != null) {
                    return applicableArtifact.getFile();
                } else {
                    throw logAndThrow("Unable to resolve automation package file " + artifactToString(getGroupId(), getArtifactId(), getArtifactClassifier(), getArtifactVersion()));
                }
            }
        };
    }

    public String getStepProjectName() {
        return stepProjectName;
    }

    public void setStepProjectName(String stepProjectName) {
        this.stepProjectName = stepProjectName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
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

    public String getArtifactClassifier() {
        return artifactClassifier;
    }

    public void setArtifactClassifier(String artifactClassifier) {
        this.artifactClassifier = artifactClassifier;
    }

    public Map<String, String> getExecutionParameters() {
        return executionParameters;
    }

    public void setExecutionParameters(Map<String, String> executionParameters) {
        this.executionParameters = executionParameters;
    }

    public Integer getExecutionResultTimeoutS() {
        return executionResultTimeoutS;
    }

    public void setExecutionResultTimeoutS(Integer executionResultTimeoutS) {
        this.executionResultTimeoutS = executionResultTimeoutS;
    }

    public Boolean getWaitForExecution() {
        return waitForExecution;
    }

    public void setWaitForExecution(Boolean waitForExecution) {
        this.waitForExecution = waitForExecution;
    }

    public Boolean getEnsureExecutionSuccess() {
        return ensureExecutionSuccess;
    }

    public void setEnsureExecutionSuccess(Boolean ensureExecutionSuccess) {
        this.ensureExecutionSuccess = ensureExecutionSuccess;
    }

    public String getIncludePlans() {
        return includePlans;
    }

    public void setIncludePlans(String includePlans) {
        this.includePlans = includePlans;
    }

    public String getExcludePlans() {
        return excludePlans;
    }

    public void setExcludePlans(String excludePlans) {
        this.excludePlans = excludePlans;
    }

}
