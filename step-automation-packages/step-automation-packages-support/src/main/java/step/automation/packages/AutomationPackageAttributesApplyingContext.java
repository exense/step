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
import step.core.objectenricher.ObjectEnricher;
import step.functions.Function;
import step.resources.ResourceManager;

import java.util.List;

public class AutomationPackageAttributesApplyingContext {
    private ResourceManager stagingResourceManager;
    private AutomationPackageArchive automationPackageArchive;
    private ObjectEnricher enricher;
    private List<Function> oldKeywords;

    private String uploadedPackageFileResource;

    public AutomationPackageAttributesApplyingContext(ResourceManager stagingResourceManager,
                                                      AutomationPackageArchive automationPackageArchive,
                                                      ObjectEnricher enricher,
                                                      List<Function> oldKeywords) {
        this.stagingResourceManager = stagingResourceManager;
        this.automationPackageArchive = automationPackageArchive;
        this.enricher = enricher;
        this.oldKeywords = oldKeywords;
    }

    public ResourceManager getStagingResourceManager() {
        return stagingResourceManager;
    }

    public void setStagingResourceManager(ResourceManager stagingResourceManager) {
        this.stagingResourceManager = stagingResourceManager;
    }

    public AutomationPackageArchive getAutomationPackageArchive() {
        return automationPackageArchive;
    }

    public String getUploadedPackageFileResource() {
        return uploadedPackageFileResource;
    }

    public void setUploadedPackageFileResource(String uploadedPackageFileResource) {
        this.uploadedPackageFileResource = uploadedPackageFileResource;
    }

    public ObjectEnricher getEnricher() {
        return enricher;
    }

    public void setAutomationPackageArchive(AutomationPackageArchive automationPackageArchive) {
        this.automationPackageArchive = automationPackageArchive;
    }

    public void setEnricher(ObjectEnricher enricher) {
        this.enricher = enricher;
    }

    public List<Function> getOldKeywords() {
        return oldKeywords;
    }

    public void setOldKeywords(List<Function> oldKeywords) {
        this.oldKeywords = oldKeywords;
    }

    public <T extends Function> T getOldKeyword(ObjectId keywordId) {
        if (oldKeywords == null) {
            return null;
        }
        try {
            Function nullableFunction = oldKeywords.stream().filter(k -> k.getId().equals(keywordId)).findFirst().orElse(null);
            return nullableFunction == null ? null : ((T) nullableFunction);
        } catch (Exception e) {
            // unable to cast old keyword to required type
            return null;
        }
    }
}
