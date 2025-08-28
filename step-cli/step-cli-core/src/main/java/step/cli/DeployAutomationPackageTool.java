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

public class DeployAutomationPackageTool extends AbstractCliTool {

    private final Params params;

    public DeployAutomationPackageTool(String url, Params params) {
        super(url);
        this.params = params;
    }

    @Override
    protected ControllerCredentials getControllerCredentials() {
        String authToken = params.getAuthToken();
        return new ControllerCredentials(getUrl(), authToken == null || authToken.isEmpty() ? null : authToken);
    }

    public void execute() throws StepCliExecutionException {
        params.validate();
        try (RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClient()) {

            AutomationPackageUpdateResult updateResult;
            String mavenAutomationPackageXml = null;
            if (params.getAutomationPackageMavenArtifact() != null) {
                mavenAutomationPackageXml = createMavenArtifactXml(params.getAutomationPackageMavenArtifact());
            }
            String mavenKeywordLibraryXml = null;
            if (params.getKeywordLibraryMavenArtifact() != null) {
                mavenKeywordLibraryXml = createMavenArtifactXml(params.getKeywordLibraryMavenArtifact());
            }

            try {
                updateResult = automationPackageClient.createOrUpdateAutomationPackage(
                        createSource(params.getAutomationPackageFile(), mavenAutomationPackageXml),
                        params.getAsync(), params.getApVersion(), params.getActivationExpression(), params.getallowUpdateOfOtherPackages(),
                        createSource(params.getKeywordLibraryFile(), mavenKeywordLibraryXml)
                );

                if (updateResult != null && updateResult.getId() != null) {
                    logInfo("Automation package successfully uploaded. With status " + updateResult.getStatus() + ". Id: " + updateResult.getId(), null);
                } else {
                    throw logAndThrow("Unexpected response from Step. The returned automation package id is null. Please check the controller logs.");
                }
            } catch (AutomationPackageClientException e){
                throw logAndThrow("Error while uploading automation package to Step: " + e.getMessage());
            }
        } catch (StepCliExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
        RemoteAutomationPackageClientImpl client = new RemoteAutomationPackageClientImpl(getControllerCredentials());
        addProjectHeaderToRemoteClient(params.getStepProjectName(), client);
        return client;
    }

    public static class Params {
        private String stepProjectName;

        private String authToken;

        private Boolean async;
        private String apVersion;
        private String activationExpression;
        private Boolean allowUpdateOfOtherPackages;
        private File automationPackageFile;
        private MavenArtifactIdentifier automationPackageMavenArtifact;

        private MavenArtifactIdentifier keywordLibraryMavenArtifact;
        private File keywordLibraryFile;

        /**
         * @return the maven snippet of automation package to be uploaded or null to use the local file instead.
         */
        public MavenArtifactIdentifier getAutomationPackageMavenArtifact() {
            return automationPackageMavenArtifact;
        }

        public Params setAutomationPackageMavenArtifact(MavenArtifactIdentifier automationPackageMavenArtifact) {
            this.automationPackageMavenArtifact = automationPackageMavenArtifact;
            return this;
        }

        public MavenArtifactIdentifier getKeywordLibraryMavenArtifact() {
            return keywordLibraryMavenArtifact;
        }

        public Params setKeywordLibraryMavenArtifact(MavenArtifactIdentifier keywordLibraryMavenArtifact) {
            this.keywordLibraryMavenArtifact = keywordLibraryMavenArtifact;
            return this;
        }

        public File getKeywordLibraryFile() {
            return keywordLibraryFile;
        }

        public Params setKeywordLibraryFile(File keywordLibraryFile) {
            this.keywordLibraryFile = keywordLibraryFile;
            return this;
        }

        public String getStepProjectName() {
            return stepProjectName;
        }

        public Params setStepProjectName(String stepProjectName) {
            this.stepProjectName = stepProjectName;
            return this;
        }

        public String getAuthToken() {
            return authToken;
        }

        public Params setAuthToken(String authToken) {
            this.authToken = authToken;
            return this;
        }

        public Boolean getAsync() {
            return async;
        }

        public Params setAsync(Boolean async) {
            this.async = async;
            return this;
        }

        public String getApVersion() {
            return apVersion;
        }

        public Params setApVersion(String apVersion) {
            this.apVersion = apVersion;
            return this;
        }

        public String getActivationExpression() {
            return activationExpression;
        }

        public Params setActivationExpression(String activationExpression) {
            this.activationExpression = activationExpression;
            return this;
        }

        public Boolean getallowUpdateOfOtherPackages() {
            return allowUpdateOfOtherPackages;
        }

        public Params setallowUpdateOfOtherPackages(Boolean allowUpdateOfOtherPackages) {
            this.allowUpdateOfOtherPackages = allowUpdateOfOtherPackages;
            return this;
        }

        public File getAutomationPackageFile() {
            return automationPackageFile;
        }

        public Params setAutomationPackageFile(File automationPackageFile) {
            this.automationPackageFile = automationPackageFile;
            return this;
        }

        public void validate() throws StepCliExecutionException {
            if (getAutomationPackageFile() != null && getAutomationPackageMavenArtifact() != null) {
                throw new StepCliExecutionException("Invalid parameters detected. The automation package should be referenced either as local file or as maven snipped");
            }
            if (getKeywordLibraryFile() != null && getKeywordLibraryMavenArtifact() != null) {
                throw new StepCliExecutionException("Invalid parameters detected. The keyword library should be referenced either as local file or as maven snipped");
            }
        }

    }
}
