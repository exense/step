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

import java.io.File;
import java.util.HashMap;
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
    public String createAutomationPackage(AutomationPackageSource automationPackageSource,
                                          String apVersion, String activationExpr, Boolean allowUpdateOfOtherPackages,
                                          AutomationPackageSource keywordLibrarySource) throws AutomationPackageClientException {
        return uploadPackage(automationPackageSource, keywordLibrarySource, multiPartEntity -> {
            Map<String, String> queryParams = new HashMap<>();
            if (allowUpdateOfOtherPackages != null) {
                queryParams.put("allowUpdateOfOtherPackages", String.valueOf(allowUpdateOfOtherPackages));
            }
            addQueryParams(apVersion, activationExpr, queryParams);
            Invocation.Builder builder = requestBuilder("/rest/automation-packages", queryParams);
            return RemoteAutomationPackageClientImpl.this.executeRequest(() -> builder.post(multiPartEntity, String.class));
        });
    }

    private void addQueryParams(String apVersion, String activationExpr, Map<String, String> queryParams) {
        if (apVersion != null && !apVersion.isEmpty()) {
            queryParams.put("version", apVersion);
        }
        if (activationExpr != null && !activationExpr.isEmpty()) {
            queryParams.put("activationExpr", activationExpr);
        }
    }

    @Override
    public AutomationPackageUpdateResult createOrUpdateAutomationPackage(AutomationPackageSource automationPackageSource,
                                                                         Boolean async, String apVersion, String activationExpr, Boolean allowUpdateOfOtherPackages,
                                                                         AutomationPackageSource keywordLibrarySource) throws AutomationPackageClientException {
        return uploadPackage(automationPackageSource, keywordLibrarySource,
                multiPartEntity -> {
                    Map<String, String> queryParams = new HashMap<>();

                    // if 'async' is not defined on client it will be resolved on the server ('false' by default)
                    if (async != null) {
                        queryParams.put("async", String.valueOf(async));
                    }
                    if (allowUpdateOfOtherPackages != null) {
                        queryParams.put("allowUpdateOfOtherPackages", String.valueOf(allowUpdateOfOtherPackages));
                    }
                    addQueryParams(apVersion, activationExpr, queryParams);
                    Invocation.Builder builder = requestBuilder("/rest/automation-packages", queryParams);
                    return RemoteAutomationPackageClientImpl.this.executeRequest(() -> builder.put(multiPartEntity, AutomationPackageUpdateResult.class));
                });
    }

    @Override
    public List<String> executeAutomationPackage(AutomationPackageSource automationPackageSource,
                                                 IsolatedAutomationPackageExecutionParameters params,
                                                 AutomationPackageSource keywordLibrarySource) throws AutomationPackageClientException {
        MultiPart multiPart = prepareFileDataMultiPart(automationPackageSource, keywordLibrarySource);
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
                                  AutomationPackageSource keywordLibrarySource,
                                  Function<Entity<MultiPart>, T> executeRequest) throws AutomationPackageClientException {
        MultiPart multiPart = prepareFileDataMultiPart(automationPackageSource, keywordLibrarySource);
        Entity<MultiPart> entity = Entity.entity(multiPart, multiPart.getMediaType());
        try {
            return executeRequest.apply(entity);
        } catch (ControllerServiceException | ControllerClientException e) {
            throw new AutomationPackageClientException(e.getMessage());
        }
    }

    private MultiPart prepareFileDataMultiPart(AutomationPackageSource automationPackageSource,
                                               AutomationPackageSource keywordLibrarySource) {

        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

        if (automationPackageSource != null && automationPackageSource.getFile() != null) {
            multiPart.bodyPart(new FileDataBodyPart("file", automationPackageSource.getFile(), MediaType.APPLICATION_OCTET_STREAM_TYPE));
        }

        if (automationPackageSource != null && automationPackageSource.getMavenSnippet() != null) {
            multiPart.bodyPart(new FormDataBodyPart("apMavenSnippet", automationPackageSource.getMavenSnippet()));
        }

        if (keywordLibrarySource != null && keywordLibrarySource.getFile() != null) {
            multiPart.bodyPart(new FileDataBodyPart("keywordLibrary", keywordLibrarySource.getFile(), MediaType.APPLICATION_OCTET_STREAM_TYPE));
        }

        if (keywordLibrarySource != null && keywordLibrarySource.getMavenSnippet() != null) {
            multiPart.bodyPart(new FormDataBodyPart("keywordLibraryMavenSnippet", keywordLibrarySource.getMavenSnippet()));
        }
        return multiPart;
    }

}
