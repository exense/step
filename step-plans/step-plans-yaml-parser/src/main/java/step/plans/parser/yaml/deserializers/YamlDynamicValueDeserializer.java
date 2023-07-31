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
package step.plans.parser.yaml.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import step.core.dynamicbeans.DynamicValue;
import step.plans.parser.yaml.YamlPlanFields;

import java.io.IOException;

public class YamlDynamicValueDeserializer extends JsonDeserializer<DynamicValue<?>> implements ContextualDeserializer {

	private JavaType type;

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		this.type = property.getType().containedType(0);
		YamlDynamicValueDeserializer deserializer = new YamlDynamicValueDeserializer();
		deserializer.type = type;
		return deserializer;
	}

	@Override
	public DynamicValue<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
		JsonNode node = jp.getCodec().readTree(jp);

		if (node.isContainerNode()) {
			JsonNode expressionNode = node.get(YamlPlanFields.DYN_VALUE_EXPRESSION_FIELD);
			String expression = expressionNode == null ? null : expressionNode.asText();

			if (expression != null) {
				// dynamic value
				return new DynamicValue<>(expression, "");
			} else {
				throw new IllegalStateException("Expression should be defined for dynamic value " + node.toPrettyString());
			}
		} else {
			// simple 'smart' mode - we can use the value explicitly without nested 'value' node
			return new DynamicValue<>(jp.getCodec().treeToValue(node, type.getRawClass()));
		}

	}

}
