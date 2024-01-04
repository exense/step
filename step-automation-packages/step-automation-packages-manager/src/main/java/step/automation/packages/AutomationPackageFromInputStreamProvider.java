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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AutomationPackageFromInputStreamProvider implements AutomationPackageArchiveProvider {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageArchiveProvider.class);

    private final AutomationPackageArchive archive;
    private File tempFile;

    public AutomationPackageFromInputStreamProvider(InputStream packageStream, String fileName) throws AutomationPackageReadingException {
        // store automation package into temp file
        this.tempFile = null;
        try {
            tempFile = stream2file(packageStream, fileName);
        } catch (Exception ex) {
            throw new AutomationPackageManagerException("Unable to store automation package file");
        }

        this.archive = new AutomationPackageArchive(tempFile, fileName);
    }

    @Override
    public AutomationPackageArchive getAutomationPackageArchive() throws AutomationPackageReadingException {
        return this.archive;
    }

    private static File stream2file(InputStream in, String fileName) throws IOException {
        final File tempFile = File.createTempFile(fileName, ".tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }

    @Override
    public void close() throws IOException {
        // cleanup temp file
        try {
            if (this.tempFile != null && tempFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        } catch (Exception e) {
            log.warn("Cannot cleanup temp file {}", this.tempFile == null ? "" : this.tempFile.getName(), e);
        }
    }
}
