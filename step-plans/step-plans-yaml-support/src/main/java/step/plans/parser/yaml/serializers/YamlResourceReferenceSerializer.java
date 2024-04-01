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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import step.core.yaml.serializers.StepYamlSerializer;
import step.core.yaml.serializers.StepYamlSerializerAddOn;
import step.plans.parser.yaml.model.YamlResourceReference;
import step.plans.parser.yaml.schema.YamlResourceReferenceJsonSchemaHelper;

import java.io.IOException;

@StepYamlSerializerAddOn(targetClasses = {YamlResourceReference.class})
public class YamlResourceReferenceSerializer extends StepYamlSerializer<YamlResourceReference> {

    public YamlResourceReferenceSerializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public void serialize(YamlResourceReference value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value.getSimpleString() != null && !value.getSimpleString().isEmpty()) {
            gen.writeString(value.getSimpleString());
        } else if (value.getResourceId() != null && !value.getResourceId().isEmpty()) {
            gen.writeStartObject();
            gen.writeStringField(YamlResourceReferenceJsonSchemaHelper.RESOURCE_REFERENCE_DEF, value.getResourceId());
            gen.writeEndObject();
        }
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, YamlResourceReference value) {
        return super.isEmpty(provider, value) || value.isEmpty();
    }
}
