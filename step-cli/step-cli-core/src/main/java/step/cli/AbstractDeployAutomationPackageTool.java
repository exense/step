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

import java.io.File;

public abstract class AbstractDeployAutomationPackageTool extends AbstractCliTool {

    private String stepProjectName;

    private String authToken;

    private Boolean async;
    private String apVersion;
    private String activationExpression;

    public AbstractDeployAutomationPackageTool(String url, String stepProjectName, String authToken, Boolean async, String apVersion, String activationExpression) {
        super(url);
        this.stepProjectName = stepProjectName;
        this.authToken = authToken;
        this.async = async;
        this.apVersion = apVersion;
        this.activationExpression = activationExpression;
    }

    @Override
    protected ControllerCredentials getControllerCredentials() {
        String authToken = getAuthToken();
        return new ControllerCredentials(getUrl(), authToken == null || authToken.isEmpty() ? null : authToken);
    }

    public void execute() throws StepCliExecutionException {
        try (RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClient()) {
            File packagedTarget = getFileToUpload();

            logInfo("Uploading the automation package...", null);
            try {
                AutomationPackageUpdateResult updateResult = automationPackageClient.createOrUpdateAutomationPackage(packagedTarget, getAsync() != null && getAsync(), getApVersion(), getActivationExpression());
                if (updateResult != null && updateResult.getId() != null) {
                    logInfo("Automation package successfully uploaded. With status " + updateResult.getStatus() + ". Id: " + updateResult.getId(), null);
                } else {
                    throw logAndThrow("Unexpected response from Step. The returned automation package id is null. Please check the controller logs.");
                }
            } catch (AutomationPackageClientException e) {
                throw logAndThrow("Error while uploading automation package to Step: " + e.getMessage());
            }
        } catch (StepCliExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    protected abstract File getFileToUpload() throws StepCliExecutionException;

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
}
