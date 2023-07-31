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
package step.plans.parser.yaml;

import step.artefacts.CallFunction;
import step.artefacts.TokenSelector;
import step.handlers.javahandler.jsonschema.DefaultFieldMetadataExtractor;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.FieldMetadataExtractor;

import java.lang.reflect.Field;

public class ArtefactFieldMetadataExtractor implements FieldMetadataExtractor {

    private final FieldMetadataExtractor defaultMetadataExtractor = new DefaultFieldMetadataExtractor();

    @Override
    public FieldMetadata extractMetadata(Field field) {
        if (field.getDeclaringClass().equals(CallFunction.class) && field.getName().equals(YamlPlanFields.CALL_FUNCTION_ARGUMENT_ORIGINAL_FIELD)) {
            // rename 'argument' field to 'inputs'
            return new FieldMetadata(YamlPlanFields.CALL_FUNCTION_ARGUMENT_SIMPLE_FIELD, null, field.getType(), false);
        } else if (field.getDeclaringClass().equals(TokenSelector.class) && field.getName().equals(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD)) {
            // rename 'token' field to 'selectionCriteria'
            return new FieldMetadata(YamlPlanFields.TOKEN_SELECTOR_TOKEN_SIMPLE_FIELD, null, field.getType(), false);
        } else if (field.getDeclaringClass().equals(CallFunction.class) && field.getName().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD)) {
            // rename 'function' field to 'keyword'
            return new FieldMetadata(YamlPlanFields.CALL_FUNCTION_FUNCTION_SIMPLE_FIELD, null, field.getType(), false);
        } else {
            return defaultMetadataExtractor.extractMetadata(field);
        }
    }
}
