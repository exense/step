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

import step.automation.packages.library.AutomationPackageLibraryProvider;
import step.core.objectenricher.ObjectPredicate;
import step.resources.ResourceManager;

public class AutomationPackageFromResourceIdProvider extends AbstractAutomationPackageFromResourceIdProvider implements AutomationPackageArchiveProvider {

    private final AutomationPackageArchive archive;

    public AutomationPackageFromResourceIdProvider(AutomationPackageReaderRegistry apReaderRegistry, ResourceManager resourceManager,
                                                   String resourceId, AutomationPackageLibraryProvider keywordLibraryProvider,
                                                   ObjectPredicate objectPredicate) {
        super(resourceManager, resourceId, objectPredicate);
        AutomationPackageReader<?> reader = apReaderRegistry.getReaderForFile(resourceFile.getResourceFile());
        try {
            this.archive = reader.createAutomationPackageArchive(resourceFile.getResourceFile(), keywordLibraryProvider == null ? null : keywordLibraryProvider.getAutomationPackageLibrary());
        } catch (AutomationPackageReadingException e) {
            throw new AutomationPackageManagerException("Unable to load automation package by resource id: " + resourceId);
        }
    }

    @Override
    public AutomationPackageArchive getAutomationPackageArchive() throws AutomationPackageReadingException {
       return archive;
    }
}
