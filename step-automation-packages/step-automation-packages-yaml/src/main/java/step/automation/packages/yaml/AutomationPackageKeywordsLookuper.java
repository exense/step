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

import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.automation.packages.model.AbstractYamlFunction;

import java.util.List;
import java.util.stream.Collectors;

public class AutomationPackageKeywordsLookuper {

    public AutomationPackageKeywordsLookuper() {
    }

    public String yamlKeywordClassToJava(String yamlKeywordClass) {
        List<Class<? extends AbstractYamlFunction<?>>> annotatedClasses = getAutomationPackageKeywords();
        for (Class<? extends AbstractYamlFunction<?>> annotatedClass : annotatedClasses) {
            String expectedYamlName = AutomationPackageNamedEntityUtils.getEntityNameByClass(annotatedClass);

            if (yamlKeywordClass.equalsIgnoreCase(expectedYamlName)) {
                return annotatedClass.getName();
            }
        }
        return null;
    }

    public List<Class<? extends AbstractYamlFunction<?>>> getAutomationPackageKeywords() {
        return AutomationPackageNamedEntityUtils.scanNamedEntityClasses(AbstractYamlFunction.class).stream()
                .map(c -> (Class<? extends AbstractYamlFunction<?>>) c)
                .collect(Collectors.toList());
    }

}
