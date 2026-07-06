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
package step.automation.packages.execution;

public class AutomationPackageExecutorException extends RuntimeException {

    public AutomationPackageExecutorException(String message) {
        super(message);
    }

    public AutomationPackageExecutorException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutomationPackageExecutorException(String message, Throwable cause, boolean addCauseToMessage) {
        // the cause is added to the message to be displayed on UI (propagated to step.core.deployment.ControllerServiceException)
        super(message + (addCauseToMessage && cause != null && cause.getMessage() != null ? " Caused by: " + cause.getMessage() : ""), cause);
    }

}
