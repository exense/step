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

import step.commons.activation.Expression;
import step.core.accessors.AbstractTrackedObject;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.core.objectenricher.EnricheableObject;

import java.util.Map;

public class AutomationPackage extends AbstractTrackedObject implements EnricheableObject {

    private AutomationPackageStatus status;
    private String versionName;
    private Expression activationExpression;

    private String automationPackageResource;
    private String automationPackageLibraryResource;
    private String automationPackageResourceRevision;
    private String automationPackageLibraryResourceRevision;

    /**
     * function attributes to be applied to all functions (aka keywords)
     */
    private Map<String, String> functionsAttributes;
    /**
     * function attributes to be applied to all plans
     */
    private Map<String, String> plansAttributes;
    /**
     * token selection criteria to be applied to all functions (aka keywords) of this package
     */
    private Map<String, String> tokenSelectionCriteria;
    /**
     * whether the keywords from this package should all be executed locally (i.e. on controller)
     */
    private boolean executeFunctionsLocally;

    public AutomationPackage(AutomationPackageStatus status, String versionName, Expression activationExpression,
                             String automationPackageResource, String automationPackageLibraryResource,
                             Map<String, String> functionsAttributes, Map<String, String> plansAttributes,
                             Map<String, String> tokenSelectionCriteria, boolean executeFunctionsLocally) {
        this.status = status;
        this.versionName = versionName;
        this.activationExpression = activationExpression;
        this.automationPackageResource = automationPackageResource;
        this.automationPackageLibraryResource = automationPackageLibraryResource;
        this.functionsAttributes = functionsAttributes;
        this.plansAttributes = plansAttributes;
        this.tokenSelectionCriteria = tokenSelectionCriteria;
        this.executeFunctionsLocally = executeFunctionsLocally;
    }

    public AutomationPackage() {
    }

    public AutomationPackageStatus getStatus() {
        return status;
    }

    public void setStatus(AutomationPackageStatus status) {
        this.status = status;
    }

    public Expression getActivationExpression() {
        return activationExpression;
    }

    public void setActivationExpression(Expression activationExpression) {
        this.activationExpression = activationExpression;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public Map<String, String> getFunctionsAttributes() {
        return functionsAttributes;
    }

    public void setFunctionsAttributes(Map<String, String> functionsAttributes) {
        this.functionsAttributes = functionsAttributes;
    }

    public Map<String, String> getPlansAttributes() {
        return plansAttributes;
    }

    public void setPlansAttributes(Map<String, String> plansAttributes) {
        this.plansAttributes = plansAttributes;
    }

    public Map<String, String> getTokenSelectionCriteria() {
        return tokenSelectionCriteria;
    }

    public void setTokenSelectionCriteria(Map<String, String> tokenSelectionCriteria) {
        this.tokenSelectionCriteria = tokenSelectionCriteria;
    }

    public boolean getExecuteFunctionsLocally() {
        return executeFunctionsLocally;
    }

    public void setExecuteFunctionsLocally(boolean executeFunctionsLocally) {
        this.executeFunctionsLocally = executeFunctionsLocally;
    }

    /**
     * @return the resource path to the package libraries. Package libraries are either a folder of jar or DLLs
     */
    @EntityReference(type= EntityManager.resources)
    public String getAutomationPackageLibraryResource() {
        return automationPackageLibraryResource;
    }

    public void setAutomationPackageLibraryResource(String automationPackageLibraryResource) {
        this.automationPackageLibraryResource = automationPackageLibraryResource;
    }

    @EntityReference(type= EntityManager.resources)
    public String getAutomationPackageResource() {
        return automationPackageResource;
    }

    public void setAutomationPackageResource(String automationPackageResource) {
        this.automationPackageResource = automationPackageResource;
    }

    public String getAutomationPackageResourceRevision() {
        return automationPackageResourceRevision;
    }

    public void setAutomationPackageResourceRevision(String automationPackageResourceRevision) {
        this.automationPackageResourceRevision = automationPackageResourceRevision;
    }

    public String getAutomationPackageLibraryResourceRevision() {
        return automationPackageLibraryResourceRevision;
    }

    public void setAutomationPackageLibraryResourceRevision(String automationPackageLibraryResourceRevision) {
        this.automationPackageLibraryResourceRevision = automationPackageLibraryResourceRevision;
    }
}
