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

import step.automation.packages.AutomationPackageArchive;
import step.automation.packages.AutomationPackageManagerException;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.InputStreamToTempFileDownloader;
import step.resources.ResourceOrigin;
import step.resources.UploadedResourceOrigin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class KeywordLibraryFromInputStreamProvider implements AutomationPackageKeywordLibraryProvider {

    private InputStreamToTempFileDownloader.TempFile tempFile;

    public KeywordLibraryFromInputStreamProvider(InputStream packageStream, String fileName) {
        // store keyword library into temp file
        try {
            tempFile = InputStreamToTempFileDownloader.copyStreamToTempFile(packageStream, fileName);
        } catch (Exception ex) {
            throw new AutomationPackageManagerException("Unable to store automation package file", ex);
        }
    }

    @Override
    public File getKeywordLibrary() throws AutomationPackageReadingException {
        return tempFile == null ? null : tempFile.getTempFile();
    }

    @Override
    public ResourceOrigin getOrigin() {
        return new UploadedResourceOrigin();
    }

    @Override
    public void close() throws IOException {
        // cleanup temp file
        InputStreamToTempFileDownloader.cleanupTempFiles(tempFile);
    }
}
