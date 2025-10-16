/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.automation.packages;

import step.core.accessors.AbstractOrganizableObject;

public class AutomationPackageAccessException extends AutomationPackageManagerException {
    public AutomationPackageAccessException(AutomationPackage automationPackage) {
        this(automationPackage, null);
    }

    public AutomationPackageAccessException(String errorMessage){
        super(errorMessage);
    }

    public AutomationPackageAccessException(AutomationPackage automationPackage, String errorMessage){
        super(getErrorMessage(automationPackage, errorMessage));
    }

    private static String getCommonErrorMessage(AutomationPackage automationPackage) {
        return "Automation package " + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + " is not acceptable";
    }

    private static String getErrorMessage(AutomationPackage automationPackage, String additionalMessage){
        if(additionalMessage == null){
            return getCommonErrorMessage(automationPackage);
        } else {
            return getCommonErrorMessage(automationPackage) + ". " + additionalMessage;
        }
    }
}
