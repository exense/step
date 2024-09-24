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
                if (isDirectory) {
                    logger.debug(resourceUrl + " is a directory. Zipping it...");
                    // If the resource is a directory, extract it and create a zip out of it
                    File zipOfDirectory;
                    try (ClassLoaderResourceFilesystem.ExtractedDirectory extractedDirectory = ClassLoaderResourceFilesystem.extractDirectory(resourceUrl)) {
                        File directory = extractedDirectory.directory;
                        String name = directory.getName();
                        // TODO the zipOfDirectory will remain on the disk until the JVM is stopped. We should properly delete it as soon as we don't need it anymore
                        zipOfDirectory = FileHelper.createTempFolder().toPath().resolve(name + ".zip").toFile();
                        FileHelper.zip(directory, zipOfDirectory);
                    }
                    resourceStream = new FileInputStream(zipOfDirectory);
                    fileName = zipOfDirectory.getName();
                } else {
                    resourceStream = context.getAutomationPackageArchive().getResourceAsStream(resourcePath);
                    fileName = resourceFile.getName();
                }

                return resourceManager.createResource(
                        resourceType,
                        resourceStream,
                        fileName,
                        false, context.getEnricher()
                );
            } catch (Exception e) {
                throw new RuntimeException("Unable to upload automation package resource " + resourcePath, e);
            }
        }

        return null;
    }
}
