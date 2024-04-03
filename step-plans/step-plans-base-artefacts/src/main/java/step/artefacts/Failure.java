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
package step.artefacts;

import step.automation.packages.AutomationPackageNamedEntity;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.plans.parser.yaml.model.YamlModel;

import java.io.PrintWriter;
import java.io.StringWriter;

@YamlModel
@AutomationPackageNamedEntity(name = "failure")
@Artefact(block = false)
public class Failure extends AbstractArtefact {

	/* We cannot add the exception as a field here, because serialization/deserialization loses type information,
	which will give wrong stacktraces. Therefore, we already have to prepare everything before the serialization.
	 */

    private String message;
    private String stackTrace;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public static Failure fromException(Throwable exception) {
        Failure f = new Failure();
        String message = "Unspecified exception";
        if (exception != null) {
            message = exception.getMessage();
            // the following should not really happen, except if someone does really stupid method overrides :-)
            if (message == null || message.isEmpty()) {
                message = exception.getClass().getName();
            }

            StringWriter w = new StringWriter();
            exception.printStackTrace(new PrintWriter(w));
            f.setStackTrace(w.toString());
        }
        f.setMessage(message);

        return f;
    }

}
