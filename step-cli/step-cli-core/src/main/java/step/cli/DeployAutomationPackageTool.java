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
import step.cli.parameters.ApDeployParameters;

public class DeployAutomationPackageTool extends AbstractCliTool<ApDeployParameters> {

    public DeployAutomationPackageTool(String url, ApDeployParameters params) {
        super(url, params);
    }

    public void execute() throws StepCliExecutionException {
        parameters.validate();
        try (RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClient()) {

            AutomationPackageUpdateResult updateResult;
            String mavenAutomationPackageXml = null;
            if (parameters.getAutomationPackageMavenArtifact() != null) {
                mavenAutomationPackageXml = createMavenArtifactXml(parameters.getAutomationPackageMavenArtifact());
            }
            String mavenPackageLibraryXml = null;
            if (parameters.getAutomationPackageLibraryMavenArtifact() != null) {
                mavenPackageLibraryXml = createMavenArtifactXml(parameters.getAutomationPackageLibraryMavenArtifact());
            }

            try {
                updateResult = automationPackageClient.createOrUpdateAutomationPackage(
                        createPackageSource(parameters.getAutomationPackageFile(), mavenAutomationPackageXml),
                        createLibrarySource(parameters.getAutomationPackageLibraryFile(), mavenPackageLibraryXml, parameters.getAutomationPackageManagedLibraryName()),
                        parameters.getApVersion(), parameters.getActivationExpression(), null, null, null, null,
                        parameters.getAsync(), parameters.getForceRefreshOfSnapshots()

                );

                if (updateResult != null && updateResult.getId() != null) {
                    logInfo("Automation package successfully uploaded. With status " + updateResult.getStatus() + ". Id: " + updateResult.getId() +
                        (updateResult.getWarnings().isEmpty() ? "" : ". Warnings: " + updateResult.getWarnings()), null);
                } else {
                    throw new StepCliExecutionException("Unexpected response from Step. The returned automation package id is null. Please check the controller logs.");
                }
            } catch (AutomationPackageClientException e){
               throw new StepCliExecutionException("Error while uploading automation package to Step: " + e.getMessage());
            }
        } catch (StepCliExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }
}
