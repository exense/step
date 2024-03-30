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
package step.artefacts.automation;

import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.schema.YamlJsonSchemaHelper;
import step.jsonschema.JsonSchema;

@JsonSchema(ref = YamlJsonSchemaHelper.DEFS_PREFIX + YamlJsonSchemaHelper.DYNAMIC_KEYWORD_INPUTS_DEF)
public class YamlDynamicInputs {

    private String json;

    public YamlDynamicInputs(String json) {
        this.json = json;
    }

    public YamlDynamicInputs() {
    }

    public DynamicValue<String> toDynamicValue() {
        return new DynamicValue<>(json);
    }

    public static YamlDynamicInputs fromDynamicValue(DynamicValue<String> dynamicValue) {
        if (dynamicValue.isDynamic()) {
            throw new UnsupportedOperationException("Dynamic arguments are not supported");
        }
        return new YamlDynamicInputs(dynamicValue.getValue());
    }

}
