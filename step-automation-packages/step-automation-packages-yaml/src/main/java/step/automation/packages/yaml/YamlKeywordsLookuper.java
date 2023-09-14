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

import step.automation.packages.AutomationPackageKeyword;
import step.automation.packages.yaml.rules.YamlKeywordConversionRule;
import step.automation.packages.yaml.rules.YamlKeywordConversionRuleMarker;
import step.core.scanner.CachedAnnotationScanner;
import step.functions.Function;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static step.core.scanner.Classes.newInstanceAs;

public class YamlKeywordsLookuper {

    public YamlKeywordsLookuper() {
    }

    public String yamlKeywordClassToJava(String yamlKeywordClass) {
        Set<Class<?>> annotatedFunctions = CachedAnnotationScanner.getClassesWithAnnotation(AutomationPackageKeyword.class);
        for (Class<?> annotatedFunction : annotatedFunctions) {
            AutomationPackageKeyword ann = annotatedFunction.getAnnotation(AutomationPackageKeyword.class);
            String expectedYamlName;
            if (ann.name() != null && !ann.name().isEmpty()) {
                expectedYamlName = ann.name();
            } else {
                expectedYamlName = annotatedFunction.getSimpleName();
            }

            if (yamlKeywordClass.equalsIgnoreCase(expectedYamlName)) {
                return annotatedFunction.getName();
            }
        }
        return null;
    }

    public List<YamlKeywordConversionRule> getConversionRulesForKeyword(Function function) {
        return getAllConversionRules().stream().filter(r -> {
            YamlKeywordConversionRuleMarker annotation = r.getClass().getAnnotation(YamlKeywordConversionRuleMarker.class);
            if (annotation != null && annotation.functions() == null) {
                return true;
            }

            Class<? extends Function>[] functions = annotation.functions();
            for (Class<? extends Function> aClass : functions) {
                if (aClass.isAssignableFrom(function.getClass())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
    }

    public List<Class<? extends Function>> getAutomationPackageKeywords() {
        return CachedAnnotationScanner.getClassesWithAnnotation(AutomationPackageKeyword.class).stream()
                .map(c -> (Class<? extends Function>) c)
                .collect(Collectors.toList());
    }

    public String getAutomationPackageKeywordName(Class<? extends Function> keywordClass) {
        boolean annotationPresent = keywordClass.isAnnotationPresent(AutomationPackageKeyword.class);
        String keywordAliasFromAnnotation = null;
        if (annotationPresent) {
            keywordAliasFromAnnotation = keywordClass.getAnnotation(AutomationPackageKeyword.class).name();
        }

        if (keywordAliasFromAnnotation == null || keywordAliasFromAnnotation.isEmpty()) {
            return keywordClass.getSimpleName();
        } else {
            return keywordAliasFromAnnotation;
        }
    }

    public List<YamlKeywordConversionRule> getAllConversionRules() {
        return CachedAnnotationScanner.getClassesWithAnnotation(YamlKeywordConversionRuleMarker.class).stream()
                .map(newInstanceAs(YamlKeywordConversionRule.class))
                .collect(Collectors.toList());
    }
}
