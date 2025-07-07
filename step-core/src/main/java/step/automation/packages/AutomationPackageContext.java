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

import step.core.objectenricher.ObjectEnricher;
import step.resources.ResourceManager;

import java.util.Map;
import java.util.Objects;

public class AutomationPackageContext {

    public static final String PLAN_ACCESSOR = "planAccessor";
    public static final String FUNCTION_ACCESSOR = "functionAccessor";

    private ResourceManager resourceManager;
    private AutomationPackageArchive automationPackageArchive;
    private AutomationPackageContent packageContent;
    private String keywordLibraryResource;

    private ObjectEnricher enricher;

    /**
     * This field should be initialized when the automation package is uploaded to Step as resource.
     * Usually it is performed in GeneralScriptFunction, where we need a reference to script (which is the AP itself).
     * So when we save the GeneralScriptFunction we need to upload the AP as resource at first
     */
    private String uploadedPackageFileResource;
    private final Map<String, Object> extensions;

    public final AutomationPackageOperationMode operationMode;

    public AutomationPackageContext(AutomationPackageOperationMode operationMode, ResourceManager resourceManager, AutomationPackageArchive automationPackageArchive,
                                    AutomationPackageContent packageContent, String keywordLibraryResource,
                                    ObjectEnricher enricher, Map<String, Object> extensions) {
        this.operationMode = Objects.requireNonNull(operationMode);
        this.resourceManager = resourceManager;
        this.automationPackageArchive = automationPackageArchive;
        this.packageContent = packageContent;
        this.keywordLibraryResource = keywordLibraryResource;
        this.enricher = enricher;
        this.extensions = extensions;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
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

    public void setEnricher(ObjectEnricher enricher) {
        this.enricher = enricher;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public AutomationPackageContent getPackageContent() {
        return packageContent;
    }

    public String getKeywordLibraryResource() {
        return keywordLibraryResource;
    }
}
