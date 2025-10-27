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
import step.core.execution.model.IsolatedAutomationPackageExecutionParameters;
import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class RemoteAutomationPackageClientImpl extends AbstractRemoteClient implements AutomationPackageClient {

    public RemoteAutomationPackageClientImpl() {
        super();
    }

    public RemoteAutomationPackageClientImpl(ControllerCredentials credentials) {
        super(credentials);
    }

    @Override
    public AutomationPackageUpdateResult createOrUpdateAutomationPackage(AutomationPackageSource automationPackageSource,
                                                                         AutomationPackageSource apLibrarySource,
                                                                         String versionName, String activationExpr,
                                                                         Map<String, String> plansAttributes, Map<String, String> functionsAttributes,
                                                                         Map<String, String> tokenSelectionCriteria, Boolean executeFunctionsLocally,
                                                                         Boolean async, Boolean forceRefreshOfSnapshots) throws AutomationPackageClientException {
        return uploadPackage(automationPackageSource, apLibrarySource, versionName, activationExpr,
                plansAttributes, functionsAttributes, tokenSelectionCriteria, executeFunctionsLocally, async, forceRefreshOfSnapshots,
                multiPartEntity -> {
                    Invocation.Builder builder = requestBuilder("/rest/automation-packages");
                    return RemoteAutomationPackageClientImpl.this.executeRequest(() -> builder.put(multiPartEntity, AutomationPackageUpdateResult.class));
                });
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
