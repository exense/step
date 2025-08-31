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

import step.automation.packages.kwlibrary.KeywordLibraryFromMavenProvider;
import step.core.maven.MavenArtifactIdentifier;
import step.core.objectenricher.ObjectPredicate;

import java.io.File;
import java.util.Map;

public class MockedAutomationPackageProvidersResolver extends AutomationPackageManager.DefaultProvidersResolver {

    private final Map<MavenArtifactIdentifier, File> mavenArtifactMocks;

    public MockedAutomationPackageProvidersResolver(Map<MavenArtifactIdentifier, File> mavenArtifactMocks){
        this.mavenArtifactMocks = mavenArtifactMocks;
    }

    @Override
    protected AutomationPackageFromMavenProvider createAutomationPackageFromMavenProvider(AutomationPackageFileSource apFileSource, ObjectPredicate predicate, AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider) {
        return new MockedAutomationPackageFromMavenProvider(mavenConfigProvider == null ? null : mavenConfigProvider.getConfig(predicate), apFileSource.getMavenArtifactIdentifier());
    }

    @Override
    protected KeywordLibraryFromMavenProvider createKeywordLibraryFromMavenProvider(AutomationPackageFileSource keywordLibrarySource, ObjectPredicate predicate, AutomationPackageMavenConfig.ConfigProvider mavenConfigProvider) {
        return new MockedKeywordLibraryFromMavenProvider(mavenConfigProvider == null ? null : mavenConfigProvider.getConfig(predicate), keywordLibrarySource.getMavenArtifactIdentifier());
    }

    public Map<MavenArtifactIdentifier, File> getMavenArtifactMocks() {
        return mavenArtifactMocks;
    }

    private class MockedAutomationPackageFromMavenProvider extends AutomationPackageFromMavenProvider {

        public MockedAutomationPackageFromMavenProvider(
                AutomationPackageMavenConfig mavenConfig,
                MavenArtifactIdentifier mavenArtifactIdentifier
        ) {
            super(mavenConfig, mavenArtifactIdentifier);
        }

        @Override
        protected File getFile() {
            return mavenArtifactMocks.get(mavenArtifactIdentifier);
        }
    }

    private class MockedKeywordLibraryFromMavenProvider extends KeywordLibraryFromMavenProvider {

        public MockedKeywordLibraryFromMavenProvider(AutomationPackageMavenConfig mavenConfig, MavenArtifactIdentifier mavenArtifactIdentifier) {
            super(mavenConfig, mavenArtifactIdentifier);
        }

        @Override
        public File getKeywordLibrary() throws AutomationPackageReadingException {
            return mavenArtifactMocks.get(mavenArtifactIdentifier);
        }
    }
}
