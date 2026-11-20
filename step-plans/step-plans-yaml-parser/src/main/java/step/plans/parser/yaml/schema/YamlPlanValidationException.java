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
package step.plans.parser.yaml.schema;

import step.artefacts.handlers.JsonSchemaValidationException;

public class YamlPlanValidationException extends RuntimeException {


    public YamlPlanValidationException(String message) {
        super(message);
    }

    public YamlPlanValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlPlanValidationException(JsonSchemaValidationException cause) {
        super("YAML Plan Validation Errors occurred:\n" + String.join("\n", cause.getFormattedErrorMessages()), cause);
    }

    public YamlPlanValidationException(JsonSchemaValidationException cause, String location) {
        super("YAML Plan Validation Errors occurred in " + location + ":\n" + String.join("\n", cause.getFormattedErrorMessages()), cause);
    }
}
