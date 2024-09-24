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
import step.core.repositories.RepositoryObjectReference;

import java.util.Map;

public class AutomationPackageExecutionParameters extends CommonExecutionParameters {

    private PlanFilter planFilter;

    private Boolean wrapIntoTestSet = false;
    private Integer numberOfThreads = 0;

    /**
     * The reference to original artifact in case of isolated execution of automation package (while the real repository is 'isolatedAutomationPackage')
     */
    private RepositoryObjectReference originalRepositoryObject;

    public AutomationPackageExecutionParameters() {
    }

    public AutomationPackageExecutionParameters(Map<String, String> customParameters, String userID, ArtefactFilter artefactFilter, PlanFilter planFilter, ExecutionMode mode) {
        super(customParameters, userID, artefactFilter, mode);
        this.planFilter = planFilter;
    }

    public ExecutionParameters toExecutionParameters(){
        ExecutionParameters params = new ExecutionParameters(getMode());
        params.setCustomParameters(getCustomParameters());
        params.setUserID(getUserID());
        params.setArtefactFilter(getArtefactFilter());
        params.setIsolatedExecution(true);
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

    public RepositoryObjectReference getOriginalRepositoryObject() {
        return originalRepositoryObject;
    }

    public void setOriginalRepositoryObject(RepositoryObjectReference originalRepositoryObject) {
        this.originalRepositoryObject = originalRepositoryObject;
    }
}
