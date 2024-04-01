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
package step.plans.parser.yaml.serializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.core.yaml.deserializers.StepYamlDeserializer;
import step.core.yaml.deserializers.StepYamlDeserializerAddOn;
import step.plans.parser.yaml.model.YamlResourceReference;
import step.plans.parser.yaml.schema.YamlResourceReferenceJsonSchemaHelper;

import java.io.IOException;

@StepYamlDeserializerAddOn(targetClasses = {YamlResourceReference.class})
public class YamlResourceReferenceDeserializer extends StepYamlDeserializer<YamlResourceReference> {

    public YamlResourceReferenceDeserializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public YamlResourceReference deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode fileReferenceField = p.getCodec().readTree(p);

        if (!fileReferenceField.isContainerNode()) {
            // TODO: simple value means the reference to local file in automation package (not supported in plans now)
            return new YamlResourceReference(fileReferenceField.asText(), null);
        }

        JsonNode resourceId = fileReferenceField.get(YamlResourceReferenceJsonSchemaHelper.FILE_REFERENCE_RESOURCE_ID_FIELD);
        if (resourceId == null) {
            throw new IllegalArgumentException("Resource id should be defined for the file reference");
        }
        return new YamlResourceReference(null, resourceId.asText());

    }
}
