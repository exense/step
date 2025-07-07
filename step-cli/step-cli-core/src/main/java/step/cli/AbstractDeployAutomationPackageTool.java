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
package step.cli;

import step.automation.packages.AutomationPackageUpdateResult;
import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.client.credentials.ControllerCredentials;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;

public abstract class AbstractDeployAutomationPackageTool extends AbstractCliTool {

    private String stepProjectName;

    private String authToken;

    private Boolean async;
    private String apVersion;
    private String activationExpression;
    private final MavenArtifactIdentifier keywordLibraryMavenArtifact;
    private final File keywordLibraryFile;

    public AbstractDeployAutomationPackageTool(String url, String stepProjectName, String authToken, Boolean async, String apVersion, String activationExpression,
                                               MavenArtifactIdentifier keywordLibraryMavenArtifact, File keywordLibraryFile) {
        super(url);
        this.stepProjectName = stepProjectName;
        this.authToken = authToken;
        this.async = async;
        this.apVersion = apVersion;
        this.activationExpression = activationExpression;
        this.keywordLibraryMavenArtifact = keywordLibraryMavenArtifact;
        this.keywordLibraryFile = keywordLibraryFile;
    }

    @Override
    protected ControllerCredentials getControllerCredentials() {
        String authToken = getAuthToken();
        return new ControllerCredentials(getUrl(), authToken == null || authToken.isEmpty() ? null : authToken);
    }

    public void execute() throws StepCliExecutionException {
        try (RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClient()) {

            AutomationPackageUpdateResult updateResult;
            MavenArtifactIdentifier mavenArtifactIdentifierToUpload = getMavenArtifactIdentifierToUpload();
            if (mavenArtifactIdentifierToUpload != null) {
                logInfo("Uploading the automation package from Maven artifactory...", null);
                if (getKeywordLibraryFile() != null) {
                    throw new StepCliExecutionException("You cannot upload the keyword library file for automation packages located in maven. You only can use a maven snippet to reference the keyword library");
                }
                try {
                    updateResult = automationPackageClient.createOrUpdateAutomationPackageMvn(
                            createMavenArtifactXml(mavenArtifactIdentifierToUpload),
                            getAsync(), getApVersion(), getActivationExpression(),
                            getKeywordLibraryMavenArtifact() == null ? null : createMavenArtifactXml(getKeywordLibraryMavenArtifact())

                    );
                } catch (AutomationPackageClientException e) {
                    throw logAndThrow("Error while uploading automation package to Step from Maven artifactory: " + e.getMessage());
                }
            } else {
                if (getKeywordLibraryMavenArtifact() != null) {
                    throw new StepCliExecutionException("You cannot the maven snipped for keyword library file. You only can use a path to the local file.");
                }
                File packagedTarget = getLocalFileToUpload();
                logInfo("Uploading the automation package...", null);
                try {
                    updateResult = automationPackageClient.createOrUpdateAutomationPackage(packagedTarget, getAsync() != null && getAsync(), getApVersion(), getActivationExpression(), getKeywordLibraryFile());
                } catch (AutomationPackageClientException e) {
                    throw logAndThrow("Error while uploading automation package to Step: " + e.getMessage());
                }
            }

            if (updateResult != null && updateResult.getId() != null) {
                logInfo("Automation package successfully uploaded. With status " + updateResult.getStatus() + ". Id: " + updateResult.getId(), null);
            } else {
                throw logAndThrow("Unexpected response from Step. The returned automation package id is null. Please check the controller logs.");
            }
        } catch (StepCliExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    protected String createMavenArtifactXml(MavenArtifactIdentifier identifier) {
        StringBuilder builder = new StringBuilder();
        builder.append("<dependency>");

        builder.append("<groupId>");
        if(identifier.getGroupId() != null){
            builder.append(identifier.getGroupId());
        }
        builder.append("</groupId>");

        builder.append("<artifactId>");
        if(identifier.getArtifactId() != null){
            builder.append(identifier.getArtifactId());
        }
        builder.append("</artifactId>");

        builder.append("<version>");
        if(identifier.getVersion() != null){
            builder.append(identifier.getVersion());
        }
        builder.append("</version>");

        builder.append("</dependency>");
        return builder.toString();
    }

    /**
     * @return the maven snippet of automation package to be uploaded or null to use the local file instead.
     */
    protected abstract MavenArtifactIdentifier getMavenArtifactIdentifierToUpload();

    protected abstract File getLocalFileToUpload() throws StepCliExecutionException;

    protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
        RemoteAutomationPackageClientImpl client = new RemoteAutomationPackageClientImpl(getControllerCredentials());
        addProjectHeaderToRemoteClient(getStepProjectName(), client);
        return client;
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

    public String getActivationExpression() {
        return activationExpression;
    }

    public void setApVersion(String apVersion) {
        this.apVersion = apVersion;
    }

    public void setActivationExpression(String activationExpression) {
        this.activationExpression = activationExpression;
    }

    public MavenArtifactIdentifier getKeywordLibraryMavenArtifact() {
        return keywordLibraryMavenArtifact;
    }

    public File getKeywordLibraryFile() {
        return keywordLibraryFile;
    }
}
