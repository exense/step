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

public class IsolatedAutomationPackageExecutionParameters extends AutomationPackageExecutionParameters {

    /**
     * The reference to original artifact in case of isolated execution of automation package (while the real repository is 'isolatedAutomationPackage')
     */
    private RepositoryObjectReference originalRepositoryObject;

    public IsolatedAutomationPackageExecutionParameters() {
        super();
    }

    public IsolatedAutomationPackageExecutionParameters(Map<String, String> customParameters, String userID, ArtefactFilter artefactFilter, PlanFilter planFilter, ExecutionMode mode) {
        super(customParameters, userID, artefactFilter, mode, planFilter);
    }

    public RepositoryObjectReference getOriginalRepositoryObject() {
        return originalRepositoryObject;
    }

    public void setOriginalRepositoryObject(RepositoryObjectReference originalRepositoryObject) {
        this.originalRepositoryObject = originalRepositoryObject;
    }

    @Override
    public ExecutionParameters toExecutionParameters() {
        ExecutionParameters params = super.toExecutionParameters();
        params.setIsolatedExecution(true);
        return params;
    }
}
