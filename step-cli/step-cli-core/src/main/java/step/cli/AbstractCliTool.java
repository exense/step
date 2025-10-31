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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.automation.packages.client.model.AutomationPackageSource;
import step.cli.parameters.Parameters;
import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;
import java.util.Objects;

public abstract class AbstractCliTool<T extends Parameters> implements CliToolLogging {

    private static final Logger log = LoggerFactory.getLogger(AbstractCliTool.class);

    private String url;

    protected final T parameters;

    public AbstractCliTool(String url, T parameters) {
        this.url = url;
        this.parameters = Objects.requireNonNull(parameters);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    protected StepCliExecutionException logAndThrow(String errorText, Throwable e) {
        logError(errorText, e);
        return new StepCliExecutionException(errorText, e);
    }

    protected StepCliExecutionException logAndThrow(String errorText) {
        logError(errorText, null);
        return new StepCliExecutionException(errorText);
    }

    @Override
    public void logError(String errorText, Throwable e) {
        if (e != null) {
            log.error(errorText, e);
        } else {
            log.error(errorText);
        }
    }

    @Override
    public void logInfo(String infoText, Throwable e) {
        if (e != null) {
            log.info(infoText, e);
        } else {
            log.info(infoText);
        }
    }

    protected ControllerCredentials getControllerCredentials() {
        String authToken = parameters.getAuthToken();
        return new ControllerCredentials(getUrl(), authToken == null || authToken.isEmpty() ? null : authToken);
    }

    protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
        RemoteAutomationPackageClientImpl client = new RemoteAutomationPackageClientImpl(getControllerCredentials());
        addProjectHeaderToRemoteClient(client);
        return client;
    }

    protected void addProjectHeaderToRemoteClient(String stepProjectName, AbstractRemoteClient remoteClient) {
        if (stepProjectName != null && !stepProjectName.isEmpty()) {
            remoteClient.getHeaders().addProjectName(stepProjectName);
        }
    }

    protected void addProjectHeaderToRemoteClient(AbstractRemoteClient remoteClient) {
        addProjectHeaderToRemoteClient(parameters.getStepProjectName(), remoteClient);
    }

    protected String createMavenArtifactXml(MavenArtifactIdentifier identifier) {
        if (identifier == null) {
            return null;
        }

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

        // classifier and type are optional fields
        if (identifier.getClassifier() != null) {
            builder.append("<classifier>");
            builder.append(identifier.getClassifier());
            builder.append("</classifier>");
        }

        if (identifier.getType() != null) {
            builder.append("<type>");
            builder.append(identifier.getType());
            builder.append("</type>");
        }

        builder.append("</dependency>");
        return builder.toString();
    }

    protected AutomationPackageSource createPackageSource(File file, String mavenSnippet) {
        return createSource(file, mavenSnippet, null);

    }

    protected AutomationPackageSource createLibrarySource(File file, String mavenSnippet, String managedLibraryName) {
        return createSource(file, mavenSnippet, managedLibraryName);
    }

    private AutomationPackageSource createSource(File file, String mavenSnippet, String managedLibraryName) {
        if (mavenSnippet != null && file != null) {
            throw new IllegalArgumentException("You cannot use both file and maven snippet as file source");
        }

        if (mavenSnippet != null) {
            return AutomationPackageSource.withMavenSnippet(mavenSnippet);
        } else if (file != null) {
            return AutomationPackageSource.withFile(file);
        } else if (managedLibraryName != null) {
            return AutomationPackageSource.withManagedLibraryName(managedLibraryName);
        } else {
            return null;
        }
    }
}
