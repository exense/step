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
package step.automation.packages.kwlibrary;

import step.attachments.FileResolver;
import step.automation.packages.AutomationPackageReadingException;
import step.core.objectenricher.ObjectPredicate;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceOrigin;
import step.resources.ResourceRevisionFileHandle;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class KeywordLibraryFromResourceIdProvider implements AutomationPackageKeywordLibraryProvider {

    private final ResourceManager resourceManager;
    private final String resourceId;

    public KeywordLibraryFromResourceIdProvider(ResourceManager resourceManager, String resourceId) {
        this.resourceManager = resourceManager;
        this.resourceId = resourceId;
    }

    @Override
    public File getKeywordLibrary() throws AutomationPackageReadingException {
        ResourceRevisionFileHandle resourceFile = resourceManager.getResourceFile(resourceId);
        return resourceFile == null ? null : resourceFile.getResourceFile();
    }

    @Override
    public ResourceOrigin getOrigin() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public boolean canLookupResources() {
        return true;
    }

    @Override
    public boolean isModifiableResource() {
        return false;
    }

    @Override
    public List<Resource> lookupExistingResources(ResourceManager resourceManager, ObjectPredicate objectPredicate) {
        Resource r = resourceManager.getResource(resourceId);
        return r == null ? List.of() : List.of(r);
    }
}
