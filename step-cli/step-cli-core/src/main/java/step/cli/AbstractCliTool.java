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

import step.client.AbstractRemoteClient;
import step.client.accessors.RemoteAccessors;
import step.client.collections.remote.RemoteCollectionFactory;
import step.client.credentials.ControllerCredentials;
import step.client.resources.RemoteResourceManager;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.entities.EntityManager;
import step.resources.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractCliTool {

    private String url;

    protected static final String ID_FIELD = AbstractIdentifiableObject.ID;

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

    protected abstract void logError(String errorText, Throwable e);

    protected abstract void logInfo(String infoText, Throwable e);

    protected ControllerCredentials getControllerCredentials() {
        return new ControllerCredentials(getUrl(), null);
    }

    protected String resolveKeywordLibResourceByCriteria(Map<String, String> libStepResourceSearchCriteria) throws StepCliExecutionException {
        logInfo("Using Step resource " + libStepResourceSearchCriteria + " as library file", null);

        if (libStepResourceSearchCriteria.containsKey(ID_FIELD)) {
            // just use the specified id
            return libStepResourceSearchCriteria.get(ID_FIELD);
        } else {
            // search resources by attributes except for id
            Map<String, String> attributes = new HashMap<>(libStepResourceSearchCriteria);
            attributes.remove(ID_FIELD);
            AbstractAccessor<Resource> remoteResourcesAccessor = createRemoteResourcesAccessor();
            List<Resource> foundResources = StreamSupport.stream(remoteResourcesAccessor.findManyByAttributes(attributes), false).collect(Collectors.toList());
            if (foundResources.isEmpty()) {
                throw new StepCliExecutionException("Library resource is not resolved by attributes: " + attributes);
            } else if (foundResources.size() > 1) {
                throw new StepCliExecutionException("Ambiguous library resources ( " + foundResources.stream().map(AbstractIdentifiableObject::getId).collect(Collectors.toList()) + " ) are resolved by attributes: " + attributes);
            } else {
                return foundResources.get(0).getId().toString();
            }
        }
    }

    protected AbstractAccessor<Resource> createRemoteResourcesAccessor() {
        RemoteAccessors remoteAccessors = new RemoteAccessors(new RemoteCollectionFactory(getControllerCredentials()));
        return remoteAccessors.getAbstractAccessor(EntityManager.resources, Resource.class);
    }


    protected RemoteResourceManager createResourceManager() {
        return new RemoteResourceManager(getControllerCredentials());
    }


    protected void addProjectHeaderToRemoteClient(String stepProjectName, AbstractRemoteClient remoteClient) {
        if (stepProjectName != null && !stepProjectName.isEmpty()) {
            remoteClient.getHeaders().addProjectName(stepProjectName);
        }
    }
}
