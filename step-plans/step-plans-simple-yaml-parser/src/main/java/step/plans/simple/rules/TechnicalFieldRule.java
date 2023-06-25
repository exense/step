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
package step.plans.simple.rules;

import jakarta.json.spi.JsonProvider;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.artefacts.AbstractArtefact;
import step.handlers.javahandler.jsonschema.FieldMetadata;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.plans.simple.serializers.SimpleArtefactFieldSerializationProcessor;

import java.lang.reflect.Field;
import java.util.Set;

public class TechnicalFieldRule implements ArtefactFieldConversionRule {
    @Override
    public JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider) {
        return (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> isTechnicalField(field, fieldMetadata);
    }

    @Override
    public SimpleArtefactFieldSerializationProcessor getArtefactFieldSerializationProcessor() {
        return (artefact, field, fieldMetadata, gen) -> isTechnicalField(field, fieldMetadata);
    }

    private boolean isTechnicalField(Field field, FieldMetadata fieldMetadata) {
        // ids and technical fields are skipped
        if (AbstractIdentifiableObject.class.equals(field.getDeclaringClass())) {
            return true;
        } else if (AbstractArtefact.class.equals(field.getDeclaringClass())) {
            // TODO: need to add some of these fields to JSON Schema (as optional fields)
            Set<String> technicalFields = Set.of(
                    "dynamicName", "useDynamicName",
                    "customAttributes", "persistNode",
                    "instrumentNode", "attachments",
                    "skipNode", "continueParentNodeExecutionOnError"
            );
            return technicalFields.contains(fieldMetadata.getFieldName());
        }
        return false;
    }


}
