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
import step.cli.parameters.LibraryDeployParameters;

public class DeployLibraryTool extends AbstractCliTool<LibraryDeployParameters> {

    public DeployLibraryTool(String url, LibraryDeployParameters params) {
        super(url, params);
    }

    public void execute() throws StepCliExecutionException {
        parameters.validate();
        try (RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClient()) {

            AutomationPackageUpdateResult updateResult;
            String mavenPackageLibraryXml = null;
            if (parameters.getLibraryMavenArtifact() != null) {
                mavenPackageLibraryXml = createMavenArtifactXml(parameters.getLibraryMavenArtifact());
            }

            try {
                updateResult = automationPackageClient.createOrUpdateAutomationPackageLibrary(
                        createLibrarySource(parameters.getLibraryFile(), mavenPackageLibraryXml, null), parameters.getManagedLibraryName());

                if (updateResult != null && updateResult.getId() != null) {
                    logInfo("Library successfully uploaded. With status " + updateResult.getStatus() + ". Id: " + updateResult.getId() +
                            (updateResult.getWarnings().isEmpty() ? "" : ". Warnings: " + updateResult.getWarnings()), null);
                } else {
                    throw new StepCliExecutionException("Unexpected response from Step. The returned library id is null. Please check the controller logs.");
                }
            } catch (AutomationPackageClientException e){
               throw new StepCliExecutionException("Error while uploading the library to Step: " + e.getMessage());
            }
        } catch (StepCliExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading the library to Step", e);
        }
    }

    protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
        RemoteAutomationPackageClientImpl client = new RemoteAutomationPackageClientImpl(getControllerCredentials());
        addProjectHeaderToRemoteClient(parameters.getStepProjectName(), client);
        return client;
    }

}
