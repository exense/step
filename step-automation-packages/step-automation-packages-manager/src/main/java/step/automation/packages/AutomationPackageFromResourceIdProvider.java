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

import step.automation.packages.kwlibrary.AutomationPackageKeywordLibraryProvider;
import step.core.objectenricher.ObjectPredicate;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.ResourceOrigin;
import step.resources.ResourceRevisionFileHandle;

import java.io.IOException;
import java.util.List;

public class AutomationPackageFromResourceIdProvider extends AbstractAutomationPackageFromResourceIdProvider implements AutomationPackageArchiveProvider {

    private final AutomationPackageKeywordLibraryProvider keywordLibraryProvider;

    public AutomationPackageFromResourceIdProvider(ResourceManager resourceManager, String resourceId, AutomationPackageKeywordLibraryProvider keywordLibraryProvider) {
        super(resourceManager, resourceId);
        this.keywordLibraryProvider = keywordLibraryProvider;
    }

    @Override
    public AutomationPackageArchive getAutomationPackageArchive() throws AutomationPackageReadingException {
        try {
            return new AutomationPackageArchive(resource.getResourceFile(), keywordLibraryProvider == null ? null : keywordLibraryProvider.getKeywordLibrary());
        } catch (AutomationPackageReadingException e) {
            throw new AutomationPackageManagerException("Unable to load automation package by resource id: " + resourceId);
        }
    }
}
