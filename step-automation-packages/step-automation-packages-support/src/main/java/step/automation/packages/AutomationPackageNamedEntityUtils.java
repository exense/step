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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
        String keywordAliasFromAnnotation = null;
        if (annotationPresent) {
            keywordAliasFromAnnotation = namedEntityClass.getAnnotation(AutomationPackageNamedEntity.class).name();
        }

        if (keywordAliasFromAnnotation == null || keywordAliasFromAnnotation.isEmpty()) {
            return namedEntityClass.getSimpleName();
        } else {
            return keywordAliasFromAnnotation;
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
}
