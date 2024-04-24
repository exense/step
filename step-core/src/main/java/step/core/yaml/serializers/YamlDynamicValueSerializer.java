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
package step.core.yaml.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.YamlFields;

import java.io.IOException;

@StepYamlSerializerAddOn(targetClasses = {DynamicValue.class})
public class YamlDynamicValueSerializer extends StepYamlSerializer<DynamicValue<?>> {

    public YamlDynamicValueSerializer() {
    }

    public YamlDynamicValueSerializer(ObjectMapper yamlObjectMapper) {
        super(yamlObjectMapper);
    }

    @Override
    public void serialize(DynamicValue value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if(value.isDynamic()){
            gen.writeStartObject();
            gen.writeStringField(YamlFields.DYN_VALUE_EXPRESSION_FIELD, value.getExpression());
            gen.writeEndObject();
        } else {
            gen.writeObject(value.getValue());
        }
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, DynamicValue<?> value) {
        // to avoid serialization for null-values
        return super.isEmpty(provider, value) || (!value.isDynamic() && value.getValue() == null) || (value.isDynamic() && value.getExpression() == null);
    }
}
