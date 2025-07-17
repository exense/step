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
import step.core.accessors.AbstractOrganizableObject;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.core.objectenricher.EnricheableObject;

public class AutomationPackage extends AbstractOrganizableObject implements EnricheableObject {

    private AutomationPackageStatus status;
    private String version;
    private Expression activationExpression;

    private String packageLibrariesLocation;

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
    public String getPackageLibrariesLocation() {
        return packageLibrariesLocation;
    }

    public void setPackageLibrariesLocation(String packageLibrariesLocation) {
        this.packageLibrariesLocation = packageLibrariesLocation;
    }
}
