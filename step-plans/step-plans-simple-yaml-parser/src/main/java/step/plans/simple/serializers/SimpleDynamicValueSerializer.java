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
package step.plans.simple.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import step.core.dynamicbeans.DynamicValue;
import step.plans.simple.YamlPlanFields;

import java.io.IOException;

public class SimpleDynamicValueSerializer extends JsonSerializer<DynamicValue> {

    @Override
    public void serialize(DynamicValue value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if(value.isDynamic()){
            gen.writeStartObject();
            gen.writeStringField(YamlPlanFields.DYN_VALUE_EXPRESSION_FIELD, value.getExpression());
            gen.writeEndObject();
        } else {
            gen.writeObject(value.getValue());
        }
    }
}
