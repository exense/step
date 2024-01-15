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
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.yaml.AutomationPackageKeywordsLookuper;
import step.automation.packages.yaml.deserialization.SpecialKeywordAttributesApplier;
import step.automation.packages.yaml.rules.YamlKeywordConversionRule;
import step.core.objectenricher.ObjectEnricher;
import step.functions.Function;
import step.resources.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AutomationPackageKeywordsAttributesApplier {

    private final AutomationPackageKeywordsLookuper lookuper = new AutomationPackageKeywordsLookuper();
    private final ResourceManager resourceManager;

    public AutomationPackageKeywordsAttributesApplier(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public List<Function> applySpecialAttributesToKeyword(List<AutomationPackageKeyword> keywords,
                                                    AutomationPackageArchive automationPackageArchive,
                                                    ObjectId automationPackageId,
                                                    ObjectEnricher objectEnricher){
        AutomationPackageAttributesApplyingContext automationPackageAttributesApplyingContext = prepareContext();
        return keywords.stream().map(keyword -> {
            List<YamlKeywordConversionRule> conversionRules = lookuper.getConversionRulesForKeyword(keyword.getDraftKeyword());
            List<SpecialKeywordAttributesApplier> appliers = conversionRules.stream()
                    .map(r -> r.getSpecialKeywordAttributesApplier(automationPackageAttributesApplyingContext))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            for (SpecialKeywordAttributesApplier applier : appliers) {
                applier.applySpecialAttributesToKeyword(keyword, automationPackageArchive, automationPackageId, objectEnricher);
            }
            return keyword.getDraftKeyword();
        }).collect(Collectors.toList());
    }

    protected AutomationPackageAttributesApplyingContext prepareContext() {
        return new AutomationPackageAttributesApplyingContext(resourceManager);
    }

}
