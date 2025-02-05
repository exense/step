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
package step.core.execution.model;

import step.core.artefacts.ArtefactFilter;
import step.core.plans.PlanFilter;

import java.util.Map;

public class AutomationPackageExecutionParameters extends CommonExecutionParameters {

    protected PlanFilter planFilter;
    private Boolean wrapIntoTestSet = false;
    private Integer numberOfThreads = 0;

    public AutomationPackageExecutionParameters() {
    }

    public AutomationPackageExecutionParameters(Map<String, String> customParameters, String userID, ArtefactFilter artefactFilter, ExecutionMode mode, PlanFilter planFilter) {
        super(customParameters, userID, artefactFilter, mode);
        this.planFilter = planFilter;
    }

    public ExecutionParameters toExecutionParameters() {
        ExecutionParameters params = new ExecutionParameters(getMode());
        params.setCustomParameters(getCustomParameters());
        params.setUserID(getUserID());
        params.setArtefactFilter(getArtefactFilter());
        return params;
    }

    public PlanFilter getPlanFilter() {
        return planFilter;
    }

    public void setPlanFilter(PlanFilter planFilter) {
        this.planFilter = planFilter;
    }

    public Boolean getWrapIntoTestSet() {
        return wrapIntoTestSet;
    }

    public void setWrapIntoTestSet(Boolean wrapIntoTestSet) {
        this.wrapIntoTestSet = wrapIntoTestSet;
    }

    public Integer getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(Integer numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }
}
