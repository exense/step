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
import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.maven.MavenArtifactIdentifier;

public abstract class AbstractCliTool implements CliToolLogging {

    private static final Logger log = LoggerFactory.getLogger(AbstractCliTool.class);

    private String url;

    public AbstractCliTool(String url) {
        this.url = url;
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
        return new ControllerCredentials(getUrl(), null);
    }

    protected void addProjectHeaderToRemoteClient(String stepProjectName, AbstractRemoteClient remoteClient) {
        if (stepProjectName != null && !stepProjectName.isEmpty()) {
            remoteClient.getHeaders().addProjectName(stepProjectName);
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
}
