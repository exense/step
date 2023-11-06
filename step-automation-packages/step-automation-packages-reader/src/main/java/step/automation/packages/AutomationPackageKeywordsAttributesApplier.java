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

import step.automation.packages.yaml.AutomationPackageKeywordsLookuper;
import step.automation.packages.yaml.deserialization.SpecialKeywordAttributesApplier;
import step.automation.packages.yaml.rules.YamlKeywordConversionRule;
import step.functions.Function;
import step.automation.packages.model.AutomationPackageKeyword;
import step.resources.ResourceManager;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AutomationPackageKeywordsAttributesApplier {

    private final AutomationPackageKeywordsLookuper lookuper = new AutomationPackageKeywordsLookuper();
    private final ResourceManager resourceManager;

    public AutomationPackageKeywordsAttributesApplier(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public Function applySpecialAttributesToKeyword(AutomationPackageKeyword keyword,
                                                   AutomationPackageArchive automationPackageArchive){
        List<YamlKeywordConversionRule> conversionRules = lookuper.getConversionRulesForKeyword(keyword.getDraftKeyword());
        List<SpecialKeywordAttributesApplier> appliers = conversionRules.stream()
                .map(r -> r.getSpecialKeywordAttributesApplier(prepareContext()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (SpecialKeywordAttributesApplier applier : appliers) {
            applier.applySpecialAttributesToKeyword(keyword, automationPackageArchive);
        }
        return keyword.getDraftKeyword();
    }

    protected AutomationPackageAttributesApplyingContext prepareContext() {
        return new AutomationPackageAttributesApplyingContext(resourceManager);
    }

}
