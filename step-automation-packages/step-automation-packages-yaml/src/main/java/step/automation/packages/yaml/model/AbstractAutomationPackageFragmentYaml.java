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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.plans.automation.YamlPlainTextPlan;
import step.plans.parser.yaml.YamlPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractAutomationPackageFragmentYaml implements AutomationPackageFragmentYaml {
    private List<String> fragments = new ArrayList<>();
    private List<YamlAutomationPackageKeyword> keywords = new ArrayList<>();
    private List<YamlPlan> plans = new ArrayList<>();
    private List<YamlPlainTextPlan> plansPlainText = new ArrayList<>();

    @JsonIgnore
    private Map<String, List<?>> additionalFields;

    @Override
    public List<YamlAutomationPackageKeyword> getKeywords() {
        return keywords;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setKeywords(List<YamlAutomationPackageKeyword> keywords) {
        this.keywords = keywords;
    }

    @Override
    public List<YamlPlan> getPlans() {
        return plans;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setPlans(List<YamlPlan> plans) {
        this.plans = plans;
    }

    @Override
    public List<String> getFragments() {
        return fragments;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setFragments(List<String> fragments) {
        this.fragments = fragments;
    }

    @Override
    public Map<String, List<?>> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(Map<String, List<?>> additionalFields) {
        this.additionalFields = additionalFields;
    }

    @Override
    public List<YamlPlainTextPlan> getPlansPlainText() {
        return plansPlainText;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setPlansPlainText(List<YamlPlainTextPlan> plansPlainText) {
        this.plansPlainText = plansPlainText;
    }
}
