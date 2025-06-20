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

import step.automation.packages.model.AutomationPackageKeyword;
import step.core.plans.Plan;

import java.util.*;

public class AutomationPackageContent {

    private String version;
    private String name;

    private List<AutomationPackageKeyword> keywords = new ArrayList<>();
    private List<Plan> plans = new ArrayList<>();
    private Map<String, List<?>> additionalData = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AutomationPackageKeyword> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<AutomationPackageKeyword> keywords) {
        this.keywords = keywords;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<Plan> getPlans() {
        return plans;
    }

    public void setPlans(List<Plan> plans) {
        this.plans = plans;
    }

    public Set<String> getAdditionalFields() {
        return additionalData.keySet();
    }

    public List<?> getAdditionalData(String fieldName) {
        return additionalData.get(fieldName);
    }

    public void addToAdditionalData(String fieldName, List<?> objects) {
        if (objects != null) {
            List<Object> existingList = (List<Object>) additionalData.computeIfAbsent(fieldName, s -> new ArrayList<>());
            existingList.addAll(objects);
        }
    }
}
