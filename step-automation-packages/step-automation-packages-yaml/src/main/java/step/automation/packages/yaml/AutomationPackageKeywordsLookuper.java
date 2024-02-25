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
package step.automation.packages.yaml;

import step.automation.packages.AutomationPackageNamedEntity;
import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.automation.packages.model.AbstractYamlKeyword;
import step.automation.packages.yaml.rules.YamlConversionRuleAddOn;
import step.automation.packages.yaml.rules.YamlKeywordConversionRule;
import step.core.scanner.CachedAnnotationScanner;

import java.util.List;
import java.util.stream.Collectors;

import static step.core.scanner.Classes.newInstanceAs;

public class AutomationPackageKeywordsLookuper {

    public AutomationPackageKeywordsLookuper() {
    }

    public String yamlKeywordClassToJava(String yamlKeywordClass) {
        List<Class<? extends AbstractYamlKeyword<?>>> annotatedClasses = getAutomationPackageKeywords();
        for (Class<? extends AbstractYamlKeyword<?>> annotatedClass : annotatedClasses) {
            AutomationPackageNamedEntity ann = annotatedClass.getAnnotation(AutomationPackageNamedEntity.class);
            String expectedYamlName;
            if (ann.name() != null && !ann.name().isEmpty()) {
                expectedYamlName = ann.name();
            } else {
                expectedYamlName = annotatedClass.getSimpleName();
            }

            if (yamlKeywordClass.equalsIgnoreCase(expectedYamlName)) {
                return annotatedClass.getName();
            }
        }
        return null;
    }

    public List<Class<? extends AbstractYamlKeyword<?>>> getAutomationPackageKeywords() {
        return AutomationPackageNamedEntityUtils.scanNamedEntityClasses(AbstractYamlKeyword.class).stream()
                .map(c -> (Class<? extends AbstractYamlKeyword<?>>) c)
                .collect(Collectors.toList());
    }

    public List<YamlKeywordConversionRule> getAllConversionRules() {
        return CachedAnnotationScanner.getClassesWithAnnotation(YamlConversionRuleAddOn.LOCATION, YamlConversionRuleAddOn.class, Thread.currentThread().getContextClassLoader()).stream()
                .filter(YamlKeywordConversionRule.class::isAssignableFrom)
                .map(newInstanceAs(YamlKeywordConversionRule.class))
                .collect(Collectors.toList());
    }
}
