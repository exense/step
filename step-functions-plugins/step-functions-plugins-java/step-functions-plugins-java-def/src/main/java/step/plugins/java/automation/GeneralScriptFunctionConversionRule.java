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

import step.automation.packages.AutomationPackageAttributesApplyingContext;
import step.automation.packages.yaml.deserialization.SpecialKeywordAttributesApplier;
import step.automation.packages.yaml.rules.YamlKeywordConversionRule;
import step.automation.packages.yaml.rules.YamlKeywordConversionRuleAddOn;
import step.core.dynamicbeans.DynamicValue;
import step.plugins.java.GeneralScriptFunction;

@YamlKeywordConversionRuleAddOn(functions = GeneralScriptFunction.class)
public class GeneralScriptFunctionConversionRule implements YamlKeywordConversionRule {

    public static final String AUTOMATION_PACKAGE_FILE_REFERENCE = "automationPackageFileReference";

    @Override
    public SpecialKeywordAttributesApplier getSpecialKeywordAttributesApplier(AutomationPackageAttributesApplyingContext context) {
        return (keyword, automationPackageArchive, automationPackageLocation) -> {
            if (keyword.getSpecialAttributes().containsKey(AUTOMATION_PACKAGE_FILE_REFERENCE)) {
                if (automationPackageLocation != null) {
                    ((GeneralScriptFunction) keyword.getDraftKeyword()).setScriptFile(new DynamicValue<>(automationPackageLocation));
                }
            }
        };
    }
}
