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

import ch.exense.commons.io.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

public class AutomationPackageResourceUploader {

    private static final Logger logger = LoggerFactory.getLogger(AutomationPackageResourceUploader.class);

    public String applyResourceReference(String resourceReference,
                                         String resourceType,
                                         AutomationPackageContext context) {
        String result = null;
        if (resourceReference != null && !resourceReference.startsWith(FileResolver.RESOURCE_PREFIX)) {
            Resource resource = uploadResourceFromAutomationPackage(resourceReference, resourceType, context);
            if (resource != null) {
                result = FileResolver.RESOURCE_PREFIX + resource.getId().toString();
            }
        } else {
            result = resourceReference;
        }
        return result;
    }

    public Resource uploadResourceFromAutomationPackage(String resourcePath,
                                                         String resourceType,
                                                         AutomationPackageContext context) {
        if (resourcePath != null && !resourcePath.isEmpty()) {
            ResourceManager resourceManager = context.getResourceManager();

            try {
                URL resourceUrl = context.getAutomationPackageArchive().getResource(resourcePath);
                if (resourceUrl == null) {
                    throw new RuntimeException("Resource not found in automation package: " + resourcePath);
                }
                File resourceFile = new File(resourceUrl.getFile());

                String fileName;
                InputStream resourceStream;
                // Check if the resource is a directory
                boolean isDirectory = ClassLoaderResourceFilesystem.isDirectory(resourceUrl);
                File tempFolder = null;
                if (isDirectory) {
                    logger.debug("The referenced resource {} is a directory. It will be extracted to a temporary directory and zipped...", resourcePath);
                    // If the resource is a directory, extract it and create a zip out of it
                    File directoryArchive;
                    // Extract the resource directory
                    try (ClassLoaderResourceFilesystem.ExtractedDirectory extractedDirectory = ClassLoaderResourceFilesystem.extractDirectory(resourceUrl)) {
                        File extractedDirectoryFile = extractedDirectory.directory;
                        String extractedDirectoryName = extractedDirectoryFile.getName();
                        // Create a temp folder as container for the archive
                        tempFolder = FileHelper.createTempFolder();
                        // Create an archive of the extracted directory
                        directoryArchive = tempFolder.toPath().resolve(extractedDirectoryName + ".zip").toFile();
                        FileHelper.zip(extractedDirectoryFile.getParentFile(), directoryArchive);
                    }
                    resourceStream = new FileInputStream(directoryArchive);
                    fileName = directoryArchive.getName();
                } else {
                    resourceStream = context.getAutomationPackageArchive().getResourceAsStream(resourcePath);
                    fileName = resourceFile.getName();
                }

                try {
                    return resourceManager.createResource(
                            resourceType,
                            false,
                            resourceStream,
                            fileName,
                            false, context.getEnricher()
                    );
                } finally {
                    resourceStream.close();
                    if (tempFolder != null) {
                        // Delete the temporary folder
                        FileHelper.deleteFolder(tempFolder);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to upload automation package resource " + resourcePath, e);
            }
        }

        return null;
    }
}
