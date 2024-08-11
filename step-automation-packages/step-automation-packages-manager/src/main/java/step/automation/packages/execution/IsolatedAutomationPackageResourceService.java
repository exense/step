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
package step.automation.packages.execution;

import step.core.objectenricher.ObjectEnricher;
import step.resources.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class IsolatedAutomationPackageResourceService {

    private final ResourceManagerImpl resourceManager;

    public IsolatedAutomationPackageResourceService(ResourceManagerImpl resourceManager){
        this.resourceManager = resourceManager;
    }

    public Resource createOrUpdateResource(String contextId, InputStream resourceStream, String resourceFileName, ObjectEnricher objectEnricher) throws IOException, InvalidResourceFormatException {
        // find by resource type and contextId (or apName and override)
        ResourceRevisionContainer resourceContainer = resourceManager.createResourceContainer(ResourceManagerImpl.RESOURCE_TYPE_ISOLATED_AP, resourceFileName);

        Resource resource = resourceContainer.getResource();
        resource.addCustomField("contextId", contextId);
        resourceManager.saveResource(resource);

        resource = resourceManager.saveResourceContent(resource.getId().toString(), resourceStream, resourceFileName);

        return resource;
    }

    public File getResourceFile(String contextId) {
        List<Resource> foundResources = resourceManager.findManyByCriteria(Map.of("customFields.contextId", contextId));
        if (foundResources.isEmpty()) {
            return null;
        }

        ResourceRevisionFileHandle fileHandle = resourceManager.getResourceFile(foundResources.get(0).getId().toString());
        if (fileHandle == null) {
            return null;
        }
        return fileHandle.getResourceFile();
    }

}
