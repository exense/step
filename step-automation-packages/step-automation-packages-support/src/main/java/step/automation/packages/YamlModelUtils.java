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

import step.core.scanner.CachedAnnotationScanner;
import step.core.yaml.YamlModel;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class YamlModelUtils {

    public static <T> List<Class<? extends T>> scanYamlModels(Class<T> applicableClass) {
        return CachedAnnotationScanner.getClassesWithAnnotation(YamlModel.LOCATION, YamlModel.class, Thread.currentThread().getContextClassLoader())
                .stream()
                .filter(applicableClass::isAssignableFrom)
                .map(c -> (Class<? extends T>) c)
                .sorted(Comparator.comparing(Class::getSimpleName))
                .collect(Collectors.toList());
    }

    public static <T> List<Class<? extends T>> scanNamedYamlModels(Class<T> applicableClass) {
        return scanYamlModels(applicableClass).stream().filter(c -> c.getAnnotation(YamlModel.class).named()).collect(Collectors.toList());
    }

    public static String getEntityNameByClass(Class<?> yamlModelClass) {
        boolean annotationPresent = yamlModelClass.isAnnotationPresent(YamlModel.class);
        String nameFromAnnotation = null;
        if (annotationPresent) {
            if(!yamlModelClass.getAnnotation(YamlModel.class).named()){
                return null;
            }
            nameFromAnnotation = yamlModelClass.getAnnotation(YamlModel.class).name();
        }

        if (nameFromAnnotation == null || nameFromAnnotation.isEmpty()) {
            return yamlModelClass.getSimpleName();
        } else {
            return nameFromAnnotation;
        }
    }

    public static <T> Class<? extends T> getClassByEntityName(String entityName, Collection<Class<? extends T>> candidates) {
        for (Class<? extends T> candidate : candidates) {
            if (Objects.equals(entityName, getEntityNameByClass(candidate))) {
                return candidate;
            }
        }
        return null;
    }

}
