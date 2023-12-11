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
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.functions.Function;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AutomationPackageTestUtils {

    public static final String PLAN_NAME_FROM_DESCRIPTOR = "Test Plan";
    public static final String PLAN_NAME_FROM_DESCRIPTOR_2 = "Test Plan 2";
    public static final String INLINE_PLAN = "inlinePlan";
    public static final String PLAN_FROM_PLANS_ANNOTATION = "plan.plan";

    public static final String J_METER_KEYWORD_1 = "JMeter keyword from automation package";
    public static final String J_METER_KEYWORD_2 = "Another JMeter keyword from automation package";
    public static final String ANNOTATED_KEYWORD = "MyKeyword2";

    public static final String SCHEDULE_1 = "firstSchedule";
    public static final String SCHEDULE_2 = "secondSchedule";

    public static AutomationPackageKeyword findKeywordByClassAndName(List<AutomationPackageKeyword> keywords, Class<?> clazz, String name) throws AssertionError {
        for (AutomationPackageKeyword keyword : keywords) {
            if (clazz.isAssignableFrom(keyword.getDraftKeyword().getClass())) {
                if (keyword.getDraftKeyword().getAttribute(AbstractOrganizableObject.NAME).equals(name)) {
                    return keyword;
                }
            }
        }
        throw new AssertionError("Keyword not found: " + clazz);
    }

    public static Plan findPlanByName(List<Plan> plans, String name) throws AssertionError {
        for (Plan plan : plans) {
            if (plan.getAttribute(AbstractOrganizableObject.NAME).equals(name)) {
                return plan;
            }
        }
        throw new AssertionError("Plan not found: " + name);
    }

    public static Function findFunctionByClassAndName(List<Function> functions, Class<?> clazz, String name) throws AssertionError {
        for (Function function : functions) {
            if (clazz.isAssignableFrom(function.getClass())) {
                if (function.getAttribute(AbstractOrganizableObject.NAME).equals(name)) {
                    return function;
                }
            }
        }
        throw new AssertionError("Function not found: " + clazz);
    }

    public static Set<ObjectId> toIds(List<? extends AbstractIdentifiableObject> objects) {
        return objects.stream().map(AbstractIdentifiableObject::getId).collect(Collectors.toSet());
    }
}
