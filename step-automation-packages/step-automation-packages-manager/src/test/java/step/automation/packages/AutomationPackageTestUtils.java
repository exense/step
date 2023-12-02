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
import step.core.accessors.AbstractIdentifiableObject;
import step.functions.Function;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AutomationPackageTestUtils {
    public static AutomationPackageKeyword findKeywordByClass(List<AutomationPackageKeyword> keywords, Class<?> clazz) {
        for (AutomationPackageKeyword keyword : keywords) {
            if (clazz.isAssignableFrom(keyword.getDraftKeyword().getClass())) {
                return keyword;
            }
        }
        throw new AssertionError("Keyword not found: " + clazz);
    }

    public static Function findFunctionByClass(List<Function> functions, Class<?> clazz) {
        for (Function keyword : functions) {
            if (clazz.isAssignableFrom(keyword.getClass())) {
                return keyword;
            }
        }
        throw new AssertionError("Function not found: " + clazz);
    }

    public static Set<ObjectId> toIds(List<Function> functions){
        return functions.stream().map(AbstractIdentifiableObject::getId).collect(Collectors.toSet());
    }
}
