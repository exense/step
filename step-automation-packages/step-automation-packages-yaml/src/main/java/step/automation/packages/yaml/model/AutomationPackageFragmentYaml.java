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
package step.automation.packages.yaml.model;

import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.plans.automation.YamlPlainTextPlan;
import step.plans.parser.yaml.YamlPlan;

import java.util.List;
import java.util.Map;

public interface AutomationPackageFragmentYaml {

    List<YamlAutomationPackageKeyword> getKeywords();

    List<YamlPlan> getPlans();

    List<YamlPlainTextPlan> getPlansPlainText();

    List<String> getFragments();

    Map<String, List<?>> getAdditionalFields();

    default <T> List<T> getAdditionalField(String k) {
        return (List<T>) getAdditionalFields().get(k);
    }
}
