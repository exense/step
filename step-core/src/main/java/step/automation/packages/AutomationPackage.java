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

public class AutomationPackage extends AbstractTrackedObject implements EnricheableObject {

    private AutomationPackageStatus status;
    private String version;
    private Expression activationExpression;

    private String automationPackageResource;
    private String keywordLibraryResource;

    public AutomationPackage(AutomationPackageStatus status, String version, Expression activationExpression, String automationPackageResource, String keywordLibraryResource) {
        this.status = status;
        this.version = version;
        this.activationExpression = activationExpression;
        this.automationPackageResource = automationPackageResource;
        this.keywordLibraryResource = keywordLibraryResource;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the resource path to the package libraries. Package libraries are either a folder of jar or DLLs
     */
    @EntityReference(type= EntityManager.resources)
    public String getKeywordLibraryResource() {
        return keywordLibraryResource;
    }

    public void setKeywordLibraryResource(String keywordLibraryResource) {
        this.keywordLibraryResource = keywordLibraryResource;
    }

    @EntityReference(type= EntityManager.resources)
    public String getAutomationPackageResource() {
        return automationPackageResource;
    }

    public void setAutomationPackageResource(String automationPackageResource) {
        this.automationPackageResource = automationPackageResource;
    }

}
