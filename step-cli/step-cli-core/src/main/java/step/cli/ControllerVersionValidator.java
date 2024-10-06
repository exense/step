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
package step.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.client.controller.ControllerServicesClient;
import step.core.Version;

public class ControllerVersionValidator {

    private static final Logger log = LoggerFactory.getLogger(ControllerVersionValidator.class);

    private final ControllerServicesClient controllerServicesClient;

    public ControllerVersionValidator(ControllerServicesClient controllerServicesClient) {
        this.controllerServicesClient = controllerServicesClient;
    }

    public Result compareVersions(Version clientVersion, Version controllerVersion) {
        if (clientVersion.getMajor() != controllerVersion.getMajor()) {
            return new Result(Status.MAJOR_MISMATCH, "The major version of your client (" + clientVersion + ") doesn't match to the Step server version (" + controllerVersion + ")");
        } else if (clientVersion.getMinor() != controllerVersion.getMinor()) {
            return new Result(Status.MINOR_MISMATCH, "The minor version of your client (" + clientVersion + ") doesn't match to the Step server version (" + controllerVersion + ")");
        } else {
            return new Result(Status.EQUAL, null);
        }
    }

    /**
     * Validates the current version of Step client (for example, CLI or Maven plugin)
     * @param clientVersion the client version
     * @return the result of validation if the mismatch is not critical
     * @throws ValidationException in case of critical mismatch between client version and server version
     */
    public Result validateVersions(Version clientVersion) throws ValidationException {
        Version controllerVersion = controllerServicesClient.getControllerVersion();

        Result r = compareVersions(clientVersion, controllerVersion);
        switch (r.getStatus()) {
            case EQUAL:
                if (r.getMessageText() != null) {
                    log.info(r.getMessageText());
                }
                return r;
            case MINOR_MISMATCH:
                if (r.getMessageText() != null) {
                    log.warn(r.getMessageText());
                }
                return r;
            default:
                if (r.getMessageText() != null) {
                    log.error(r.getMessageText());
                }
                throw new ValidationException(r.getMessageText(), r.getStatus());
        }
    }

    public enum Status {
        EQUAL,
        MAJOR_MISMATCH,
        MINOR_MISMATCH
    }

    public static class Result {
        private final Status status;
        private final String messageText;

        public Result(Status status, String messageText) {
            this.status = status;
            this.messageText = messageText;
        }

        public Status getStatus() {
            return status;
        }

        public String getMessageText() {
            return messageText;
        }
    }

    public static class ValidationException extends java.lang.Exception {
        private Status status;

        public ValidationException(String message, Status status) {
            super(message);
            this.status = status;
        }

        public Status getStatus() {
            return status;
        }
    }
}
