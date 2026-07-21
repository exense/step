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
package step.automation.packages.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import step.automation.packages.AutomationPackageUpdateResult;
import step.automation.packages.client.model.AutomationPackageSource;
import step.client.ControllerClientException;
import step.controller.services.async.AsyncTaskStatus;
import step.core.execution.model.IsolatedAutomationPackageExecutionParameters;
import step.client.AbstractRemoteClient;
import step.client.RemoteClientConfiguration;
import step.client.credentials.ControllerCredentials;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class RemoteAutomationPackageClientImpl extends AbstractRemoteClient implements AutomationPackageClient {

    /**
     * Interval between two polls of the deployment task status.
     */
    private static final long DEPLOYMENT_POLL_INTERVAL_MS = 2000L;

    public RemoteAutomationPackageClientImpl() {
        super();
    }

    public RemoteAutomationPackageClientImpl(ControllerCredentials credentials) {
        super(credentials);
    }

    public RemoteAutomationPackageClientImpl(RemoteClientConfiguration configuration) {
        super(configuration);
    }

    @Override
    public AutomationPackageUpdateResult createOrUpdateAutomationPackage(AutomationPackageSource automationPackageSource,
                                                                         AutomationPackageSource apLibrarySource,
                                                                         String versionName, String activationExpr,
                                                                         Map<String, String> plansAttributes, Map<String, String> functionsAttributes,
                                                                         Map<String, String> tokenSelectionCriteria, Boolean executeFunctionsLocally,
                                                                         Boolean async, Boolean forceRefreshOfSnapshots,
                                                                         long deploymentTimeoutMs) throws AutomationPackageClientException {
        // The deployment endpoint is asynchronous: it immediately returns an AsyncTaskStatus that we poll until the
        // deployment (which may wait for running executions to release the package) reaches a final state.
        AsyncTaskStatus<AutomationPackageUpdateResult> initialStatus = uploadPackage(automationPackageSource, apLibrarySource, versionName, activationExpr,
            plansAttributes, functionsAttributes, tokenSelectionCriteria, executeFunctionsLocally, async, forceRefreshOfSnapshots,
            multiPartEntity -> {
                Invocation.Builder builder = requestBuilder("/rest/automation-packages");
                return RemoteAutomationPackageClientImpl.this.executeRequest(() -> builder.put(multiPartEntity, new GenericType<AsyncTaskStatus<AutomationPackageUpdateResult>>() {
                }));
            });
        return waitForDeploymentTask(initialStatus, deploymentTimeoutMs);
    }

    /**
     * Polls the deployment task status until it becomes ready, then returns its result or throws the server-side error.
     *
     * @param deploymentTimeoutMs max time to wait for completion; a value &lt;= 0 means waiting indefinitely
     */
    private AutomationPackageUpdateResult waitForDeploymentTask(AsyncTaskStatus<AutomationPackageUpdateResult> initialStatus, long deploymentTimeoutMs) throws AutomationPackageClientException {
        if (initialStatus == null || initialStatus.getId() == null) {
            throw new AutomationPackageClientException("Unexpected response from Step. The deployment task id is null. Please check the controller logs.");
        }

        // A non-positive timeout means "wait indefinitely". We compare elapsed time against the timeout rather than
        // computing an absolute deadline, which avoids any overflow when very large timeout values are passed.
        long startTime = System.currentTimeMillis();
        AsyncTaskStatus<AutomationPackageUpdateResult> status = initialStatus;
        while (!status.isReady()) {
            if (deploymentTimeoutMs > 0 && System.currentTimeMillis() - startTime > deploymentTimeoutMs) {
                throw new AutomationPackageClientException("Timeout while waiting for the automation package deployment to complete after "
                    + (deploymentTimeoutMs / 1000) + "s. The deployment may still be running on the server (task id: " + initialStatus.getId() + ").");
            }
            try {
                Thread.sleep(DEPLOYMENT_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AutomationPackageClientException("Interrupted while waiting for the automation package deployment to complete.");
            }
            status = getAsyncTaskStatus(initialStatus.getId());
            if (status == null) {
                throw new AutomationPackageClientException("The deployment task (" + initialStatus.getId() + ") could not be found on the server anymore. Please check the controller logs.");
            }
        }

        if (status.getError() != null) {
            throw new AutomationPackageClientException(status.getError());
        }
        return status.getResult();
    }

    private AsyncTaskStatus<AutomationPackageUpdateResult> getAsyncTaskStatus(String taskId) throws AutomationPackageClientException {
        Invocation.Builder builder = requestBuilder("/rest/async/" + taskId);
        return executeAutomationPackageClientRequest(() -> builder.get(new GenericType<AsyncTaskStatus<AutomationPackageUpdateResult>>() {
        }));
    }

    @Override
    public List<String> executeAutomationPackage(AutomationPackageSource automationPackageSource,
                                                 IsolatedAutomationPackageExecutionParameters params,
                                                 AutomationPackageSource apLibrarySource) throws AutomationPackageClientException {
        MultiPart multiPart = prepareFileDataMultiPart(automationPackageSource, apLibrarySource);
        FormDataBodyPart paramsBodyPart = new FormDataBodyPart("executionParams", params, MediaType.APPLICATION_JSON_TYPE);
        multiPart.bodyPart(paramsBodyPart);

        Entity<MultiPart> entity = Entity.entity(multiPart, multiPart.getMediaType());
        Invocation.Builder builder = requestBuilder("/rest/automation-packages/execute");
        return this.executeAutomationPackageClientRequest(() -> builder.post(entity, new GenericType<List<String>>() {
        }));
    }

    @Override
    public void deleteAutomationPackage(String packageId) throws AutomationPackageClientException {
        Invocation.Builder builder = requestBuilder("/rest/automation-packages/" + packageId);
        executeAutomationPackageClientRequest(() -> builder.delete(Void.class));
    }

    @Override
    public AutomationPackageUpdateResult createOrUpdateAutomationPackageLibrary(AutomationPackageSource librarySource, String managedLibraryName) throws AutomationPackageClientException {
        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        if (librarySource != null && librarySource.getFile() != null) {
            multiPart.bodyPart(new FileDataBodyPart("file", librarySource.getFile(), MediaType.APPLICATION_OCTET_STREAM_TYPE));
        }
        if (librarySource != null && librarySource.getMavenSnippet() != null) {
            multiPart.bodyPart(new FormDataBodyPart("mavenSnippet", librarySource.getMavenSnippet()));
        }
        addStringBodyPart("managedLibraryName", managedLibraryName, multiPart);
        Entity<MultiPart> entity = Entity.entity(multiPart, multiPart.getMediaType());
        Invocation.Builder builder = requestBuilder("/rest/automation-packages/library");
        return this.executeAutomationPackageClientRequest(() -> builder.post(entity, AutomationPackageUpdateResult.class));
    }

    private <T> T executeAutomationPackageClientRequest(Supplier<T> provider) throws AutomationPackageClientException {
        try {
            return executeRequest(provider);
        } catch (ControllerServiceException | ControllerClientException e) {
            throw new AutomationPackageClientException(e.getMessage());
        }
    }

    protected <T> T uploadPackage(AutomationPackageSource automationPackageSource,
                                  AutomationPackageSource apLibrarySource,
                                  String versionName, String activationExpr,
                                  Map<String, String> plansAttributes, Map<String, String> functionsAttributes,
                                  Map<String, String> tokenSelectionCriteria, Boolean executeFunctionsLocally, Boolean async, Boolean forceRefreshOfSnapshots,
                                  Function<Entity<MultiPart>, T> executeRequest) throws AutomationPackageClientException {
        MultiPart multiPart = prepareFileDataMultiPart(automationPackageSource, apLibrarySource);

        addStringBodyPart("versionName", versionName, multiPart);
        addStringBodyPart("activationExpr", activationExpr, multiPart);
        // Signal to the server that this client supports the asynchronous deployment protocol (i.e. it will poll the
        // returned async task). Servers from a previous minor version simply ignore this unknown form part. Omitting
        // it (as previous-minor clients do) makes the server fall back to the legacy synchronous response.
        addBooleanBodyPart("asyncDeployment", true, multiPart);
        addBooleanBodyPart("async", async, multiPart);
        addBooleanBodyPart("forceRefreshOfSnapshots", forceRefreshOfSnapshots, multiPart);
        addMapBodyPart("plansAttributes", plansAttributes, multiPart);
        addMapBodyPart("functionsAttributes", functionsAttributes, multiPart);
        addMapBodyPart("tokenSelectionCriteria", tokenSelectionCriteria, multiPart);
        addBooleanBodyPart("executeFunctionsLocally", executeFunctionsLocally, multiPart);

        Entity<MultiPart> entity = Entity.entity(multiPart, multiPart.getMediaType());
        try {
            return executeRequest.apply(entity);
        } catch (ControllerServiceException | ControllerClientException e) {
            throw new AutomationPackageClientException(e.getMessage());
        }
    }

    private static void addStringBodyPart(String fieldName, String value, MultiPart multiPart) {
        if (value != null) {
            multiPart.bodyPart(new FormDataBodyPart(fieldName, value));
        }
    }

    private static void addBooleanBodyPart(String fieldName, Boolean value, MultiPart multiPart) {
        if (value != null) {
            multiPart.bodyPart(new FormDataBodyPart(fieldName, String.valueOf(value)));
        }
    }

    private static void addMapBodyPart(String fieldName, Map<String, String> value, MultiPart multiPart) throws AutomationPackageClientException {
        if (value != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                multiPart.bodyPart(new FormDataBodyPart(fieldName, objectMapper.writeValueAsString(value)));
            } catch (JsonProcessingException e) {
                throw new AutomationPackageClientException("Unable to serialize the plans attributes. Reason: " + e.getMessage());
            }
        }
    }


    private MultiPart prepareFileDataMultiPart(AutomationPackageSource automationPackageSource,
                                               AutomationPackageSource apLibrarySource) {

        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

        if (automationPackageSource != null && automationPackageSource.getFile() != null) {
            multiPart.bodyPart(new FileDataBodyPart("file", automationPackageSource.getFile(), MediaType.APPLICATION_OCTET_STREAM_TYPE));
        }

        if (automationPackageSource != null && automationPackageSource.getMavenSnippet() != null) {
            multiPart.bodyPart(new FormDataBodyPart("apMavenSnippet", automationPackageSource.getMavenSnippet()));
        }

        if (apLibrarySource != null && apLibrarySource.getFile() != null) {
            multiPart.bodyPart(new FileDataBodyPart("apLibrary", apLibrarySource.getFile(), MediaType.APPLICATION_OCTET_STREAM_TYPE));
        }

        if (apLibrarySource != null && apLibrarySource.getMavenSnippet() != null) {
            multiPart.bodyPart(new FormDataBodyPart("apLibraryMavenSnippet", apLibrarySource.getMavenSnippet()));
        }

        if (apLibrarySource != null && apLibrarySource.getManagedLibraryName() != null) {
            multiPart.bodyPart(new FormDataBodyPart("managedLibraryName", apLibrarySource.getManagedLibraryName()));
        }
        return multiPart;
    }

}
