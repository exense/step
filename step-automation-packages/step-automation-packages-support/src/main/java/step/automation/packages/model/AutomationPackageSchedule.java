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
package step.automation.packages.model;

import java.util.Map;

public class AutomationPackageSchedule {

    private String name;
    private Boolean active = true;
    private String cron;
    private String planName;
    private String assertionPlanName;
    private Map<String, String> executionParameters;

    public AutomationPackageSchedule() {
    }

    public AutomationPackageSchedule(String name, String cron, String planName, Map<String, String> executionParameters) {
        this.name = name;
        this.cron = cron;
        this.planName = planName;
        this.executionParameters = executionParameters;
    }

    public String getCron() {
        return cron;
    }

    public String getName() {
        return name;
    }

    public String getPlanName() {
        return planName;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Map<String, String> getExecutionParameters() {
        return executionParameters;
    }

    public void setExecutionParameters(Map<String, String> executionParameters) {
        this.executionParameters = executionParameters;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getAssertionPlanName() {
        return assertionPlanName;
    }

    public void setAssertionPlanName(String assertionPlanName) {
        this.assertionPlanName = assertionPlanName;
    }


    @Override
    public String toString() {
        return "AutomationPackageSchedule{" +
                "name='" + name + '\'' +
                ", active=" + active +
                ", cron='" + cron + '\'' +
                ", planName='" + planName + '\'' +
                ", assertionPlanName='" + assertionPlanName + '\'' +
                ", executionParameters=" + executionParameters +
                '}';
    }
}
