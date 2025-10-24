/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.automation.packages;

import step.core.objectenricher.ObjectPredicate;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceOrigin;
import step.resources.ResourceRevisionFileHandle;

import java.io.IOException;
import java.util.Optional;

public class AbstractAutomationPackageFromResourceIdProvider implements AutomationPackageProvider {

    protected final String resourceId;
    protected final ResourceRevisionFileHandle resourceFile;

    public AbstractAutomationPackageFromResourceIdProvider(ResourceManager resourceManager, String resourceId, ObjectPredicate objectPredicate) {
        this.resourceId = resourceId;
        Resource resource = resourceManager.getResource(resourceId);
        if (resource == null) {
            throw new AutomationPackageManagerException("Automation package resource hasn't been found by ID: " + resourceId);
        } else {
            this.resourceFile = resourceManager.getResourceFile(resourceId);
            if (resourceFile == null) {
                throw new AutomationPackageManagerException("Automation package resource file hasn't been found by ID: " + resourceId);
            } else if (!objectPredicate.test(resource)) {
                throw new AutomationPackageManagerException("The referenced automation package resource is not accessible in current context");
            }
        }
    }

    @Override
    public ResourceOrigin getOrigin() {
        return null;
    }

    @Override
    public void close() throws IOException {
        if (resourceFile != null) {
            resourceFile.close();
        }
    }

    @Override
    public boolean isModifiableResource() {
        return false;
    }

    @Override
    public boolean canLookupResources() {
        return true;
    }

    @Override
    public Optional<Resource> lookupExistingResource(ResourceManager resourceManager, ObjectPredicate objectPredicate) {
        Resource r = resourceManager.getResource(resourceId);
        return (r == null || !objectPredicate.test(r)) ? Optional.empty() : Optional.of(r);
    }
}
