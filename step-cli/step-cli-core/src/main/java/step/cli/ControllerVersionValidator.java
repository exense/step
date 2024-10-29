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

import step.client.controller.ControllerServicesClient;
import step.core.Version;

public class ControllerVersionValidator {

    private final ControllerServicesClient controllerServicesClient;

    public ControllerVersionValidator(ControllerServicesClient controllerServicesClient) {
        this.controllerServicesClient = controllerServicesClient;
    }

    public static Result compareVersions(Version clientVersion, Version controllerVersion) {
        // For Step we're using the minor version as major. We're therefore reporting a minor mismatch as major here
        if ((clientVersion.getMajor() != controllerVersion.getMajor()) || (clientVersion.getMinor() != controllerVersion.getMinor())) {
            return new Result(controllerVersion, clientVersion, Status.MAJOR_MISMATCH);
        } else if (clientVersion.getRevision() != controllerVersion.getRevision()) {
            return new Result(controllerVersion, clientVersion, Status.MINOR_MISMATCH);
        } else {
            return new Result(controllerVersion, clientVersion, Status.EQUAL);
        }
    }

    /**
     * Validates the current version of Step client (for example, CLI or Maven plugin)
     * @param clientVersion the client version
     * @return the result of validation if the mismatch is not critical
     * @throws ValidationException in case of mismatch between client version and server version
     */
    public Result validateVersions(Version clientVersion) throws ValidationException {
        Version controllerVersion = controllerServicesClient.getControllerVersion();
        Result r = compareVersions(clientVersion, controllerVersion);
        switch (r.getStatus()) {
            case EQUAL:
                return r;
            default:
                throw new ValidationException(r);
        }
    }

    public enum Status {
        EQUAL,
        MAJOR_MISMATCH,
        MINOR_MISMATCH
    }

    public static class Result {

        private Status status;
        private Version serverVersion;
        private Version clientVersion;

        public Result(Version serverVersion, Version clientVersion, Status status) {
            this.status = status;
            this.serverVersion = serverVersion;
            this.clientVersion = clientVersion;
        }

        public Status getStatus() {
            return status;
        }

        public Version getServerVersion() {
            return serverVersion;
        }

        public Version getClientVersion() {
            return clientVersion;
        }

    }

    public static class ValidationException extends java.lang.Exception {

        private final Result result;

        public ValidationException(Result result) {
            super();
            this.result = result;
        }

        public Result getResult() {
            return result;
        }
    }
}
