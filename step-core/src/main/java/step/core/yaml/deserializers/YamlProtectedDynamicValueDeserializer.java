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
package step.core.yaml.deserializers;

import com.fasterxml.jackson.databind.*;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.ProtectedDynamicValue;

@StepYamlDeserializerAddOn(targetClasses = {ProtectedDynamicValue.class})
public class YamlProtectedDynamicValueDeserializer extends YamlDynamicValueDeserializer {

	public YamlProtectedDynamicValueDeserializer() {
	}

	public YamlProtectedDynamicValueDeserializer(ObjectMapper yamlObjectMapper) {
		super(yamlObjectMapper);
	}

	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
		this.type = property.getType().containedType(0);
		YamlProtectedDynamicValueDeserializer deserializer = new YamlProtectedDynamicValueDeserializer();
		deserializer.type = type;
		return deserializer;
	}

	@Override
	protected DynamicValue<?> getDynamicValue(Object o) {
		return new ProtectedDynamicValue<>(o);
	}

	@Override
	protected DynamicValue<Object> getDynamicValueWithExpresion(String expression) {
		return new ProtectedDynamicValue<>(expression, "");
	}
}
