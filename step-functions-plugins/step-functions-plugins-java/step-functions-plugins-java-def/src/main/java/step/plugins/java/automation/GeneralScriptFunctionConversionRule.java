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
package step.plugins.java.automation;

import step.attachments.FileResolver;
import step.automation.packages.AutomationPackageAttributesApplyingContext;
import step.automation.packages.yaml.deserialization.SpecialKeywordAttributesApplier;
import step.automation.packages.yaml.rules.YamlKeywordConversionRule;
import step.automation.packages.yaml.rules.YamlKeywordConversionRuleAddOn;
import step.core.dynamicbeans.DynamicValue;
import step.plugins.java.GeneralScriptFunction;
import step.resources.InvalidResourceFormatException;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.SimilarResourceExistingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@YamlKeywordConversionRuleAddOn(functions = GeneralScriptFunction.class)
public class GeneralScriptFunctionConversionRule implements YamlKeywordConversionRule {

    public static final String AUTOMATION_PACKAGE_FILE_REFERENCE = "automationPackageFileReference";

    @Override
    public SpecialKeywordAttributesApplier getSpecialKeywordAttributesApplier(AutomationPackageAttributesApplyingContext context) {
        return (keyword, automationPackageArchive, automationPackageId, objectEnricher) -> {
            GeneralScriptFunction generalScriptFunction = (GeneralScriptFunction) keyword.getDraftKeyword();
            if (generalScriptFunction.getScriptFile().get() == null || generalScriptFunction.getScriptFile().get().isEmpty()) {
                String uploadedPackageFileResource = context.getUploadedPackageFileResource();
                if (uploadedPackageFileResource == null) {
                    File originalFile = automationPackageArchive.getOriginalFile();
                    if (originalFile == null) {
                        throw new RuntimeException("General script functions can only be used within automation package archive");
                    }
                    try (InputStream is = new FileInputStream(originalFile)) {
                        Resource resource = context.getResourceManager().createResource(
                                ResourceManager.RESOURCE_TYPE_FUNCTIONS, is, originalFile.getName(), false, objectEnricher
                        );
                        uploadedPackageFileResource = FileResolver.RESOURCE_PREFIX + resource.getId().toString();

                        // fill context with just uploaded resource to upload it only once and reuse it in other general script functions
                        context.setUploadedPackageFileResource(uploadedPackageFileResource);
                    } catch (IOException | SimilarResourceExistingException | InvalidResourceFormatException e) {
                        throw new RuntimeException("General script function cannot be created", e);
                    }
                }
                generalScriptFunction.setScriptFile(new DynamicValue<>(uploadedPackageFileResource));
            }
        };
    }
}
