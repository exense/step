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

public class AutomationPackageSchedulerTask {

    private String name;
    private String cron;
    private String planName;
    private String environment;

    public AutomationPackageSchedulerTask() {
    }

    public AutomationPackageSchedulerTask(String name, String cron, String planName, String environment) {
        this.name = name;
        this.cron = cron;
        this.planName = planName;
        this.environment = environment;
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

    public String getEnvironment() {
        return environment;
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

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @Override
    public String toString() {
        return "AutomationPackageSchedulerTask{" +
                "name='" + name + '\'' +
                ", cron='" + cron + '\'' +
                ", planName='" + planName + '\'' +
                ", environment='" + environment + '\'' +
                '}';
    }
}
