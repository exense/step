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
package step.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectionUtils {

    /**
     * Analyzes the class hierarchy and writes all applicable fields to the json schema (output)
     * @param untilParentClassIs ignores all fields of this parent class and all it's parent classes
     */
    public static List<Field> getAllFieldsInHierarchy(Class<?> clazz, Class<?> untilParentClassIs) {
        List<Field> allFieldsInHierarchy = new ArrayList<>();
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            allFieldsInHierarchy.addAll(Stream.of(currentClass.getDeclaredFields()).filter(f -> !f.isSynthetic() && !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList()));
            currentClass = currentClass.getSuperclass();
            if (currentClass != null && currentClass.equals(untilParentClassIs)) {
                currentClass = null;
            }
        }
        Collections.reverse(allFieldsInHierarchy);
        return allFieldsInHierarchy;
    }
}
