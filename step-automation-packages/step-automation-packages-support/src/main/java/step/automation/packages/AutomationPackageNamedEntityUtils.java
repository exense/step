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

import jakarta.json.spi.JsonProvider;
import step.automation.packages.yaml.rules.YamlConversionRule;
import step.automation.packages.yaml.rules.YamlConversionRuleAddOn;
import step.core.scanner.CachedAnnotationScanner;
import step.core.yaml.deserializers.YamlFieldDeserializationProcessor;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;

import java.util.*;
import java.util.stream.Collectors;

import static step.core.scanner.Classes.newInstanceAs;

public class AutomationPackageNamedEntityUtils {

    public static List<Class<?>> scanNamedEntityClasses(Class<?> applicableClass) {
        return CachedAnnotationScanner.getClassesWithAnnotation(AutomationPackageNamedEntity.LOCATION, AutomationPackageNamedEntity.class, Thread.currentThread().getContextClassLoader())
                .stream()
                .filter(applicableClass::isAssignableFrom)
                .sorted(Comparator.comparing(Class::getSimpleName))
                .collect(Collectors.toList());
    }

    public static String getEntityNameByClass(Class<?> namedEntityClass) {
        boolean annotationPresent = namedEntityClass.isAnnotationPresent(AutomationPackageNamedEntity.class);
        String nameFromAnnotation = null;
        if (annotationPresent) {
            nameFromAnnotation = namedEntityClass.getAnnotation(AutomationPackageNamedEntity.class).name();
        }

        if (nameFromAnnotation == null || nameFromAnnotation.isEmpty()) {
            return namedEntityClass.getSimpleName();
        } else {
            return nameFromAnnotation;
        }
    }

    public static Class<?> getClassByEntityName(String entityName, Collection<Class<?>> candidates) {
        for (Class<?> candidate : candidates) {
            if (entityName.equals(getEntityNameByClass(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    public static List<YamlFieldDeserializationProcessor> scanDeserializationProcessorsForNamedEntity(Class<?> namedEntityClass) {
        // scan all deserialization processors from classpath
        List<YamlConversionRule> rules = getConversionRulesForNamedEntity(namedEntityClass);

        List<YamlFieldDeserializationProcessor> res = new ArrayList<>();
        for (YamlConversionRule rule : rules) {
            YamlFieldDeserializationProcessor p = rule.getDeserializationProcessor();
            if (p != null) {
                res.add(p);
            }
        }
        return res;
    }

    public static List<JsonSchemaFieldProcessor> scanJsonSchemaFieldProcessorsForNamedEntity(Class<?> namedEntityClass, JsonProvider jsonProvider) {
        // scan all json schema field processors from classpath
        List<YamlConversionRule> rules = getConversionRulesForNamedEntity(namedEntityClass);

        List<JsonSchemaFieldProcessor> res = new ArrayList<>();
        for (YamlConversionRule rule : rules) {
            JsonSchemaFieldProcessor proc = rule.getJsonSchemaFieldProcessor(jsonProvider);
            if (proc != null) {
                res.add(proc);
            }
        }
        return res;
    }

    private static List<YamlConversionRule> getConversionRulesForNamedEntity(Class<?> namedEntityClass) {
        return CachedAnnotationScanner.getClassesWithAnnotation(YamlConversionRuleAddOn.LOCATION, YamlConversionRuleAddOn.class, Thread.currentThread().getContextClassLoader()).stream()
                .filter(c -> {
                    Class<?>[] targetClasses = c.getAnnotation(YamlConversionRuleAddOn.class).targetClasses();
                    return targetClasses == null || targetClasses.length == 0 || Arrays.stream(targetClasses).anyMatch(namedEntityClass::isAssignableFrom);
                })
                .map(newInstanceAs(YamlConversionRule.class))
                .collect(Collectors.toList());
    }
}
