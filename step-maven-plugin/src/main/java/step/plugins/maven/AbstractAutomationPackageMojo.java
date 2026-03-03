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
package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;

public abstract class AbstractAutomationPackageMojo extends AbstractStepPluginMojo {

    protected abstract LibraryConfiguration getLibrary();

    protected abstract String getLibraryGroupId();

    protected abstract String getLibraryArtifactId();

    protected abstract String getLibraryVersion();

    protected abstract String getLibraryClassifier();

    protected abstract String getLibraryType();

    protected abstract String getLibraryPath();

    protected abstract String getLibraryManaged();

    protected LibraryConfiguration prepareLibrary() throws MojoExecutionException {
        // Merge flat properties into the library object,
        // letting flat properties (e.g. from system properties) take precedence over structured XML config.
        LibraryConfiguration library = getLibrary();
        if (library == null) {
            library = new LibraryConfiguration();
        }
        if (getLibraryGroupId() != null)    library.setGroupId(getLibraryGroupId());
        if (getLibraryArtifactId() != null) library.setArtifactId(getLibraryArtifactId());
        if (getLibraryVersion() != null)    library.setVersion(getLibraryVersion());
        if (getLibraryClassifier() != null) library.setClassifier(getLibraryClassifier());
        if (getLibraryType() != null)       library.setType(getLibraryType());
        if (getLibraryPath() != null)       library.setPath(getLibraryPath());
        if (getLibraryManaged() != null)    library.setManaged(getLibraryManaged());

        if (library.isSet()) {
            library.validate();
        }
        return library;
    }
}
