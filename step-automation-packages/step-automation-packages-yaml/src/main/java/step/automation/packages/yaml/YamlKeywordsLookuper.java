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

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import step.automation.packages.yaml.rules.YamlKeywordConversionRule;
import step.automation.packages.yaml.rules.YamlKeywordConversionRuleMarker;
import step.core.scanner.CachedAnnotationScanner;
import step.functions.Function;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static step.core.scanner.Classes.newInstanceAs;

public class YamlKeywordsLookuper {

    private final Reflections functionReflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage("step")));

    public YamlKeywordsLookuper() {
    }

    public String yamlKeywordClassToJava(String yamlKeywordClass) {
        Set<Class<? extends Function>> functionImpls = functionReflections.getSubTypesOf(Function.class);
        for (Class<? extends Function> functionImpl : functionImpls) {
            if (functionImpl.getSimpleName().equalsIgnoreCase(yamlKeywordClass)) {
                return functionImpl.getName();
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

    public List<YamlKeywordConversionRule> getAllConversionRules() {
        return CachedAnnotationScanner.getClassesWithAnnotation(YamlKeywordConversionRuleMarker.class).stream()
                .map(newInstanceAs(YamlKeywordConversionRule.class))
                .collect(Collectors.toList());
    }
}
