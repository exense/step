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
package step.core.plans.serialization;

import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.handlers.javahandler.jsonschema.JsonInputConverter;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

class SimpleDynamicValueJsonSchemaHelper {

	private static final Logger log = LoggerFactory.getLogger(SimpleDynamicValueJsonSchemaHelper.class);

	private static final String DYNAMIC_VALUE_STRING_DEF = "DynamicValueStringDef";
	private static final String DYNAMIC_VALUE_NUM_DEF = "DynamicValueNumDef";
	private static final String DYNAMIC_VALUE_BOOLEAN_DEF = "DynamicValueBooleanDef";
	private final JsonProvider jsonProvider;

	public SimpleDynamicValueJsonSchemaHelper(JsonProvider jsonProvider) {
		this.jsonProvider = jsonProvider;
	}

	public Map<String, JsonObjectBuilder> createDynamicValueImplDefs() {
		Map<String, JsonObjectBuilder> res = new HashMap<>();
		res.put(DYNAMIC_VALUE_STRING_DEF, createDynamicValueDef("string"));
		res.put(DYNAMIC_VALUE_NUM_DEF, createDynamicValueDef("number"));
		res.put(DYNAMIC_VALUE_BOOLEAN_DEF, createDynamicValueDef("boolean"));
		return res;
	}

	private JsonObjectBuilder createDynamicValueDef(String valueType) {
		JsonObjectBuilder res = jsonProvider.createObjectBuilder();
		res.add("type", "object");
		JsonObjectBuilder properties = jsonProvider.createObjectBuilder();
		properties.add("expression", jsonProvider.createObjectBuilder().add("type", "string"));
		properties.add("value", jsonProvider.createObjectBuilder().add("type", valueType));
		res.add("properties", properties);
		return res;
	}

	/**
	 * Uses a reference to one of dynamic value definitions (depending on generic type of field)
	 */
	public void applyDynamicValueDefForField(Field field, JsonObjectBuilder propertiesBuilder){
		// for dynamic value we need to reference the definition prepared above
		// the definition depends on generic type used in dynamic value (string, integer, boolean, etc)
		Type genericType = field.getGenericType();
		if (genericType instanceof ParameterizedType) {
			Type[] arguments = ((ParameterizedType) genericType).getActualTypeArguments();
			Type dynamicValueClass = arguments[0];
			if (!(dynamicValueClass instanceof Class)) {
				throw new IllegalArgumentException("Unsupported dynamic value type " + dynamicValueClass);
			}
			String dynamicValueType = JsonInputConverter.resolveJsonPropertyType((Class<?>) dynamicValueClass);
			switch (dynamicValueType){
				case "string":
					SimplifiedPlanJsonSchemaGenerator.addRef(propertiesBuilder, DYNAMIC_VALUE_STRING_DEF);
					break;
				case "boolean":
					SimplifiedPlanJsonSchemaGenerator.addRef(propertiesBuilder, DYNAMIC_VALUE_BOOLEAN_DEF);
					break;
				case "number":
					SimplifiedPlanJsonSchemaGenerator.addRef(propertiesBuilder, DYNAMIC_VALUE_NUM_DEF);
					break;
				case "object":
					log.warn("Unknown dynamic value type for field " + field.getName());
					SimplifiedPlanJsonSchemaGenerator.addRef(propertiesBuilder, DYNAMIC_VALUE_STRING_DEF);
					break;
				default:
					throw new IllegalArgumentException("Unsupported dynamic value type: " + dynamicValueType);
			}

		} else {
			throw new IllegalArgumentException("Unsupported dynamic value generic field " + genericType);
		}
	}

}
