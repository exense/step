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
package step.parameter.automation;

import step.parameter.Parameter;

public class AutomationPackageParameter {

    // the field name in automation package yaml
    public static final String FIELD_NAME_IN_AP = "parameters";

    // the subschema name in json schema
    public static final String DEF_NAME_IN_JSON_SCHEMA = "StepParameterDef";

    protected String key;
    protected String value;
    protected String description;

    public Parameter toParameter() {
        Parameter res = new Parameter();
        res.setKey(key);
        res.setValue(value);
        res.setDescription(description);
        // ...
        return res;
    }
}
