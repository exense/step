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

import ch.exense.commons.io.FileHelper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamToTempFileDownloader {

    private static final Logger log = LoggerFactory.getLogger(InputStreamToTempFileDownloader.class);

    public static TempFile copyStreamToTempFile(InputStream in, String fileName) throws IOException {
        // create temp folder to keep the original file name
        File newFolder = FileHelper.createTempFolder();
        newFolder.deleteOnExit();
        File newFile = new File(newFolder, fileName);
        newFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(newFile)) {
            IOUtils.copy(in, out);
        }
        return new TempFile(newFile, newFolder);
    }

    public static void cleanupTempFiles(TempFile temp) {
        // cleanup temp file
        try {
            if (temp != null && temp.getTempFile() != null && temp.getTempFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                temp.getTempFile().delete();
            }
            if (temp != null && temp.getTempFolder() != null && temp.getTempFolder().exists()) {
                //noinspection ResultOfMethodCallIgnored
                temp.getTempFolder().delete();
            }
        } catch (Exception e) {
            log.warn("Cannot cleanup temp file {}", (temp == null || temp.getTempFile() == null) ? "" : temp.getTempFile().getName(), e);
        }
    }

    public static class TempFile {
        private File tempFile;
        private File tempFolder;

        public TempFile(File tempFile, File tempFolder) {
            this.tempFile = tempFile;
            this.tempFolder = tempFolder;
        }

        public File getTempFile() {
            return tempFile;
        }

        public File getTempFolder() {
            return tempFolder;
        }
    }
}
