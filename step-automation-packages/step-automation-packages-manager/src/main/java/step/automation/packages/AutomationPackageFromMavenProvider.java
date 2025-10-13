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
package step.automation.packages;

import step.automation.packages.library.AutomationPackageLibraryProvider;
import step.core.maven.MavenArtifactIdentifier;
import step.core.objectenricher.ObjectPredicate;
import step.resources.ResourceManager;

public class AutomationPackageFromMavenProvider extends AbstractAutomationPackageFromMavenProvider implements AutomationPackageArchiveProvider {

    private final AutomationPackageArchive archive;

    public AutomationPackageFromMavenProvider(AutomationPackageMavenConfig mavenConfig,
                                              MavenArtifactIdentifier mavenArtifactIdentifier,
                                              AutomationPackageLibraryProvider keywordLibraryProvider,
                                              ResourceManager resourceManager, ObjectPredicate objectPredicate) throws AutomationPackageReadingException {
        super(mavenConfig, mavenArtifactIdentifier, resourceManager, objectPredicate);
        this.archive = new AutomationPackageArchive(resolvedMavenArtefact.artifactFile, keywordLibraryProvider == null ? null : keywordLibraryProvider.getAutomationPackageLibrary());
    }

    @Override
    public AutomationPackageArchive getAutomationPackageArchive()  {
        return archive;
    }


}
