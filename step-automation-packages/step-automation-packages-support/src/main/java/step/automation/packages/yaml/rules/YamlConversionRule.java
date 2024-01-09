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
package step.automation.packages.yaml.rules;

import jakarta.json.spi.JsonProvider;
import step.core.yaml.deserializers.YamlFieldDeserializationProcessor;
import step.handlers.javahandler.jsonschema.FieldMetadataExtractor;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;

public interface YamlConversionRule {

    /**
     * Returns the field metadata provider (to decide, how the field should be named in yaml format, it the field mandatory etc)
     */
    default FieldMetadataExtractor getFieldMetadataExtractor() {
        return null;
    }

    /**
     * Returns the json schema field processor to be used in when generating the json schema for entity (if the fields
     * requires some special non-standard representation in json schema)
     */
    default JsonSchemaFieldProcessor getJsonSchemaFieldProcessor(JsonProvider jsonProvider){
        return null;
    }

    /**
     * Returns the processor to be used to deserialize the yaml representation to the technical object
     */
    default YamlFieldDeserializationProcessor getDeserializationProcessor() {
        return null;
    }
}
