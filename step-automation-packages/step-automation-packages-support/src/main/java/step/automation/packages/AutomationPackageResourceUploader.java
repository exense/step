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

import step.resources.Resource;
import step.resources.ResourceManager;

import java.io.File;
import java.net.URL;

public class AutomationPackageResourceUploader {

    public Resource uploadResourceFromAutomationPackage(String resourcePath,
                                                        String resourceType,
                                                        AutomationPackageAttributesApplyingContext context) {
        if (resourcePath != null && !resourcePath.isEmpty()) {
            ResourceManager resourceManager = context.getResourceManager();

            try {
                URL resourceUrl = context.getAutomationPackageArchive().getResource(resourcePath);
                if (resourceUrl == null) {
                    throw new RuntimeException("Resource not found in automation package: " + resourcePath);
                }
                File resourceFile = new File(resourceUrl.getFile());
                return resourceManager.createResource(
                        resourceType,
                        context.getAutomationPackageArchive().getResourceAsStream(resourcePath),
                        resourceFile.getName(),
                        false, context.getEnricher()
                );
            } catch (Exception e) {
                throw new RuntimeException("Unable to upload automation package resource " + resourcePath, e);
            }
        }

        return null;
    }
}
