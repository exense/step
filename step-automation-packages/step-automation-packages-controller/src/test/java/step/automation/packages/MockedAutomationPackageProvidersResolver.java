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
import step.automation.packages.library.AutomationPackageLibraryFromMavenProvider;
import step.core.maven.MavenArtifactIdentifier;
import step.core.objectenricher.ObjectPredicate;
import step.repositories.artifact.ResolvedMavenArtifact;
import step.resources.ResourceManager;

import java.util.Map;

public class MockedAutomationPackageProvidersResolver extends AutomationPackageManager.DefaultProvidersResolver {

    private final Map<MavenArtifactIdentifier, ResolvedMavenArtifact> mavenArtifactMocks;

    public MockedAutomationPackageProvidersResolver(Map<MavenArtifactIdentifier, ResolvedMavenArtifact> mavenArtifactMocks, ResourceManager resourceManager){
        super(resourceManager);
        this.mavenArtifactMocks = mavenArtifactMocks;
    }

    @Override
    protected AutomationPackageFromMavenProvider createAutomationPackageFromMavenProvider(AutomationPackageFileSource apFileSource,
                                                                                          ObjectPredicate predicate,
                                                                                          AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider,
                                                                                          AutomationPackageLibraryProvider apLibraryProvider,
                                                                                          ResourceManager resourceManager) throws AutomationPackageReadingException {
        return new MockedAutomationPackageFromMavenProvider(
                mavenConfigProvider == null ? null : mavenConfigProvider.getConfig(),
                apFileSource.getMavenArtifactIdentifier(),
                apLibraryProvider
        );
    }

    @Override
    protected AutomationPackageLibraryFromMavenProvider createAutomationPackageLibraryFromMavenProvider(AutomationPackageFileSource apLibrarySource, ObjectPredicate predicate, AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider, ResourceManager resourceManager) throws AutomationPackageReadingException {
        return new MockedAutomationPackageLibraryFromMavenProvider(mavenConfigProvider == null ? null :
                mavenConfigProvider.getConfig(), apLibrarySource.getMavenArtifactIdentifier(),
                resourceManager, predicate);
    }

    public Map<MavenArtifactIdentifier, ResolvedMavenArtifact> getMavenArtifactMocks() {
        return mavenArtifactMocks;
    }

    private class MockedAutomationPackageFromMavenProvider extends AutomationPackageFromMavenProvider {

        public MockedAutomationPackageFromMavenProvider(
                AutomationPackageMavenConfig mavenConfig,
                MavenArtifactIdentifier mavenArtifactIdentifier,
                AutomationPackageLibraryProvider keywordLibraryProvider
        ) throws AutomationPackageReadingException {
            super(mavenConfig, mavenArtifactIdentifier, keywordLibraryProvider, null, null);
        }


        @Override
        protected ResolvedMavenArtifact getResolvedMavenArtefact() {
            return mavenArtifactMocks.get(mavenArtifactIdentifier);
        }
    }

    private class MockedAutomationPackageLibraryFromMavenProvider extends AutomationPackageLibraryFromMavenProvider {

        public MockedAutomationPackageLibraryFromMavenProvider(AutomationPackageMavenConfig mavenConfig, MavenArtifactIdentifier mavenArtifactIdentifier, ResourceManager resourceManager, ObjectPredicate objectPredicate) throws AutomationPackageReadingException {
            super(mavenConfig, mavenArtifactIdentifier, resourceManager, objectPredicate);
        }

        @Override
        protected ResolvedMavenArtifact getResolvedMavenArtefact() {
            return mavenArtifactMocks.get(mavenArtifactIdentifier);
        }
    }
}
