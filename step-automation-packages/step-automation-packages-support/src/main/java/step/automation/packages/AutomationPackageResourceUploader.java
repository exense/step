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

import org.bson.types.ObjectId;
import step.attachments.FileResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectEnricher;
import step.resources.InvalidResourceFormatException;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.SimilarResourceExistingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class AutomationPackageResourceUploader {

    public String applyResourceReference(String resourceReference,
                                         String resourceType,
                                         AutomationPackageAttributesApplyingContext context,
                                         DynamicValue<String> oldResourceReference){
        String result = null;
        if (resourceReference != null && !resourceReference.startsWith(FileResolver.RESOURCE_PREFIX)) {
            Resource resource = uploadResourceFromAutomationPackage(resourceReference, resourceType, context, oldResourceReference);
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
                                                        AutomationPackageAttributesApplyingContext context,
                                                        DynamicValue<String> oldResourceReference) {
        if (resourcePath != null && !resourcePath.isEmpty()) {
            ResourceManager stagingResourceManager = context.getStagingResourceManager();

            try {
                URL resourceUrl = context.getAutomationPackageArchive().getResource(resourcePath);
                if (resourceUrl == null) {
                    throw new RuntimeException("Resource not found in automation package: " + resourcePath);
                }
                File resourceFile = new File(resourceUrl.getFile());
                InputStream is = context.getAutomationPackageArchive().getResourceAsStream(resourcePath);
                return uploadResource(resourceType, is,  resourceFile.getName(), stagingResourceManager, oldResourceReference, context.getEnricher());
            } catch (Exception e) {
                throw new RuntimeException("Unable to upload automation package resource " + resourcePath, e);
            }
        }

        return null;
    }

    public Resource uploadResource(String resourceType, InputStream is, String fileName,
                                   ResourceManager resourceManager,
                                   DynamicValue<String> oldResourceReference,
                                   ObjectEnricher enricher) throws IOException, InvalidResourceFormatException, SimilarResourceExistingException {
        FileResolver fr = new FileResolver(resourceManager);
        String oldResourceId = null;
        if (oldResourceReference != null && oldResourceReference.getValue() != null && !oldResourceReference.getValue().isEmpty()) {
            oldResourceId = fr.resolveResourceId(oldResourceReference.getValue());
        }

        // we need to reuse old resource id (if exists)
        return resourceManager.createResource(oldResourceId == null ? null : new ObjectId(oldResourceId), resourceType, false, is, fileName, false, enricher, null);
    }
}
