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
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import step.automation.packages.execution.AutomationPackageExecutionParameters;
import step.automation.packages.execution.ExecuteAutomationPackageResult;
import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;

import java.io.File;
import java.util.function.Function;

public class RemoteAutomationPackageClientImpl extends AbstractRemoteClient implements AutomationPackageClient {

    public RemoteAutomationPackageClientImpl() {
        super();
    }

    public RemoteAutomationPackageClientImpl(ControllerCredentials credentials) {
        super(credentials);
    }

    @Override
    public String createAutomationPackage(File automationPackageFile) {
        return uploadPackage(automationPackageFile, multiPartEntity -> {
            Invocation.Builder builder = requestBuilder("/rest/automation-packages");
            return RemoteAutomationPackageClientImpl.this.executeRequest(() -> builder.post(multiPartEntity, String.class));
        });
    }

    @Override
    public String createOrUpdateAutomationPackage(File automationPackageFile) {
        return uploadPackage(automationPackageFile, multiPartEntity -> {
            Invocation.Builder builder = requestBuilder("/rest/automation-packages");
            return RemoteAutomationPackageClientImpl.this.executeRequest(() -> builder.put(multiPartEntity, String.class));
        });
    }

    @Override
    public ExecuteAutomationPackageResult executeAutomationPackage(File automationPackageFile, AutomationPackageExecutionParameters params) {
        MultiPart multiPart = prepareFileDataMultiPart(automationPackageFile);
        FormDataBodyPart paramsBodyPart = new FormDataBodyPart("executionParams", params, MediaType.APPLICATION_JSON_TYPE);
        multiPart.bodyPart(paramsBodyPart);

        Entity<MultiPart> entity = Entity.entity(multiPart, multiPart.getMediaType());
        Invocation.Builder builder = requestBuilder("/rest/automation-packages/execute");
        return builder.post(entity, ExecuteAutomationPackageResult.class);
    }

    @Override
    public void deleteAutomationPackage(String packageId) {
        Invocation.Builder builder = requestBuilder("/rest/automation-packages/" + packageId);
        executeRequest(() -> builder.delete(Void.class));
    }


    protected String uploadPackage(File automationPackageFile, Function<Entity<MultiPart>, String> executeRequest) {
        MultiPart multiPart = prepareFileDataMultiPart(automationPackageFile);
        Entity<MultiPart> entity = Entity.entity(multiPart, multiPart.getMediaType());
        return executeRequest.apply(entity);
    }

    private MultiPart prepareFileDataMultiPart(File automationPackageFile) {
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", automationPackageFile, MediaType.APPLICATION_OCTET_STREAM_TYPE);

        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(fileDataBodyPart);
        return multiPart;
    }

}
