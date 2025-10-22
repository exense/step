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
import step.core.objectenricher.ObjectAccessException;

public class AutomationPackageAccessException extends AutomationPackageManagerException {

    public AutomationPackageAccessException(String errorMessage){
        super(errorMessage);
    }

    public AutomationPackageAccessException(String errorMessage, ObjectAccessException objectAccessException) {
        super(errorMessage + ". Access violation reason: " + objectAccessException.getMessage());
    }

    public AutomationPackageAccessException(AutomationPackage automationPackage, String errorMessage, ObjectAccessException objectAccessException) {
        super(getErrorMessage(automationPackage, errorMessage, objectAccessException));
    }

    private static String getCommonErrorMessage(AutomationPackage automationPackage) {
        return "Automation package " + automationPackage.getAttribute(AbstractOrganizableObject.NAME) + " is not acceptable";
    }

    private static String getErrorMessage(AutomationPackage automationPackage, String additionalMessage, ObjectAccessException objectAccessException){
        String finalMessage = getCommonErrorMessage(automationPackage);
        if(additionalMessage != null){
            finalMessage += ". " + additionalMessage;
        }
        if (objectAccessException != null) {
            finalMessage += ". Access violation reason: " + objectAccessException.getMessage();
        }
        return finalMessage;
    }
}
