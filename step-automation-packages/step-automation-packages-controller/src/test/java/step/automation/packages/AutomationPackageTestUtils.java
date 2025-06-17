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
import step.automation.packages.model.AbstractYamlFunction;
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.model.JavaAutomationPackageKeyword;
import step.automation.packages.model.YamlAutomationPackageKeyword;
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
    public static final String PLAN_NAME_FROM_DESCRIPTOR_PLAIN_TEXT = "Plain text plan";
    public static final String PLAN_NAME_PLAIN_TEXT_2 = "firstPlainText.plan";
    public static final String PLAN_NAME_PLAIN_TEXT_3 = "secondPlainText.plan";
    public static final String PLAN_NAME_WITH_COMPOSITE = "Test Plan with Composite";
    public static final String INLINE_PLAN = "Inline Plan";
    public static final String PLAN_FROM_PLANS_ANNOTATION = "plan.plan";

    public static final String J_METER_KEYWORD_1 = "JMeter keyword from automation package";
    public static final String J_METER_KEYWORD_2 = "Another JMeter keyword from automation package";
    public static final String COMPOSITE_KEYWORD = "Composite keyword from AP";
    public static final String GENERAL_SCRIPT_KEYWORD = "GeneralScript keyword from AP";
    public static final String NODE_KEYWORD = "NodeAutomation";
    public static final String ANNOTATED_KEYWORD = "MyKeyword2";

    public static final String SCHEDULE_1 = "firstSchedule";
    public static final String SCHEDULE_2 = "secondSchedule";

    public static Function findJavaKeywordByClassAndName(List<AutomationPackageKeyword> keywords, Class<?> clazz, String name) throws AssertionError {
        for (AutomationPackageKeyword keyword : keywords) {
            if (keyword instanceof JavaAutomationPackageKeyword) {
                Function wrappedKeyword = ((JavaAutomationPackageKeyword) keyword).getKeyword();
                if (clazz.isAssignableFrom(wrappedKeyword.getClass())) {
                    if (wrappedKeyword.getAttribute(AbstractOrganizableObject.NAME).equals(name)) {
                        return wrappedKeyword;
                    }
                }
            }
        }
        throw new AssertionError("Keyword not found: " + clazz);
    }

    public static AbstractYamlFunction<?> findYamlKeywordByClassAndName(List<AutomationPackageKeyword> keywords, Class<?> clazz, String name) throws AssertionError {
        for (AutomationPackageKeyword keyword : keywords) {
            if (keyword instanceof YamlAutomationPackageKeyword) {
                AbstractYamlFunction<?> yamlKeyword = ((YamlAutomationPackageKeyword) keyword).getYamlKeyword();
                if (clazz.isAssignableFrom(yamlKeyword.getClass())) {
                    if (yamlKeyword.getName().equals(name)) {
                        return yamlKeyword;
                    }
                }
            }
        }
        throw new AssertionError("Keyword not found: " + clazz);
    }

    public static <T extends AbstractOrganizableObject> T findByName(List<T> objects, String name){
        for (T object : objects) {
            if (object.getAttribute(AbstractOrganizableObject.NAME).equals(name)) {
                return object;
            }
        }
        throw new AssertionError("Object not found: " + name);
    }

    public static Plan findPlanByName(List<Plan> plans, String name) throws AssertionError {
        return findByName(plans, name);
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
