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
package step.core.yaml.schema;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.yaml.YamlFields;
import step.handlers.javahandler.jsonschema.JsonInputConverter;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class YamlJsonSchemaHelper {

	private static final Logger log = LoggerFactory.getLogger(YamlJsonSchemaHelper.class);

	private static final String DYNAMIC_EXPRESSION_DEF = "DynamicExpressionDef";

	public static final String SMART_DYNAMIC_VALUE_STRING_DEF = "SmartDynamicValueStringDef";
	public static final String SMART_DYNAMIC_VALUE_NUM_DEF = "SmartDynamicValueNumDef";
	public static final String SMART_DYNAMIC_VALUE_BOOLEAN_DEF = "SmartDynamicValueBooleanDef";

	public static final String DYNAMIC_KEYWORD_INPUTS_DEF = "DynamicKeywordInputsDef";
	private final JsonProvider jsonProvider;

	public YamlJsonSchemaHelper(JsonProvider jsonProvider) {
		this.jsonProvider = jsonProvider;
	}

	public Map<String, JsonObjectBuilder> createDynamicValueImplDefs() {
		Map<String, JsonObjectBuilder> res = new HashMap<>();
		res.put(DYNAMIC_EXPRESSION_DEF, createDynamicValueDef());
		res.put(SMART_DYNAMIC_VALUE_STRING_DEF, createSmartDynamicValueDef("string"));
		res.put(SMART_DYNAMIC_VALUE_NUM_DEF, createSmartDynamicValueDef("number"));
		res.put(SMART_DYNAMIC_VALUE_BOOLEAN_DEF, createSmartDynamicValueDef("boolean"));
		res.put(DYNAMIC_KEYWORD_INPUTS_DEF, createDynamicKeywordInputsDef());
		return res;
	}

	/**
	 * Prepares the json schema for class.
	 * @return the following structure:
	 * {
	 *   "type": "object",
	 *   "properties": {
	 *     fields extracted from class via reflection
	 *   },
	 *   "additionalProperties": false,
	 *   "required": [...]
	 * }
	 */
	public JsonObjectBuilder createJsonSchemaForClass(JsonSchemaCreator jsonSchemaCreator, Class<?> clazz, boolean additionalProperties) throws JsonSchemaPreparationException {
		JsonObjectBuilder res = jsonProvider.createObjectBuilder();
		res.add("type", "object");

		JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();

		List<String> requiredProperties = new ArrayList<>();
		extractPropertiesFromClass(jsonSchemaCreator, clazz, propertiesBuilder, requiredProperties);

		res.add("properties", propertiesBuilder);
		if(!additionalProperties) {
			res.add("additionalProperties", additionalProperties);
		}
		addRequiredProperties(requiredProperties, res);
		return res;
	}

	/**
	 * Analyzes the class hierarchy and writes all applicable fields to the json schema (output)
	 */
	public void extractPropertiesFromClass(JsonSchemaCreator jsonSchemaCreator, Class<?> clazz, JsonObjectBuilder output, List<String> requiredPropertiesOutput) throws JsonSchemaPreparationException {
		log.info("Preparing json schema for class {}...", clazz);

		// analyze the class hierarchy
		List<Field> allFieldsInHierarchy = new ArrayList<>();
		Class<?> currentClass = clazz;
		while (currentClass != null) {
			allFieldsInHierarchy.addAll(List.of(currentClass.getDeclaredFields()));
			currentClass = currentClass.getSuperclass();
		}
		Collections.reverse(allFieldsInHierarchy);

		// for each field we want either build the json schema via reflection
		// or use some predefined schemas for some special classes (like step.core.dynamicbeans.DynamicValue)
		try {
			jsonSchemaCreator.processFields(clazz, output, allFieldsInHierarchy, requiredPropertiesOutput);
		} catch (Exception ex) {
			throw new JsonSchemaPreparationException("Unable to json schema for class " + clazz, ex);
		}
	}

	protected void addRequiredProperties(List<String> requiredProperties, JsonObjectBuilder propertiesBuilder) {
		if (requiredProperties != null && !requiredProperties.isEmpty()) {
			JsonArrayBuilder requiredBuilder = jsonProvider.createArrayBuilder();
			for (String requiredProperty : requiredProperties) {
				requiredBuilder.add(requiredProperty);
			}
			propertiesBuilder.add("required", requiredBuilder);
		}
	}

	private JsonObjectBuilder createDynamicKeywordInputsDef(){
//		{
//			"type" : "array",
//			"items" : {
//			"type" : "object",
//					"patternProperties" : {
//				".*" : {
//					"oneOf" : [ {
//						"type" : "number"
//					}, {
//						"type" : "boolean"
//					}, {
//						"type" : "string"
//					}, {
//						"$ref" : "#/$defs/DynamicExpressionDef"
//					} ]
//				}
//			},
//			"additionalProperties": false
//		}
//		}
		JsonObjectBuilder res = jsonProvider.createObjectBuilder();
		res.add("type", "array");
		JsonObjectBuilder arrayItemDef = jsonProvider.createObjectBuilder();
		arrayItemDef.add("type", "object");

		JsonArrayBuilder oneOfArray = jsonProvider.createArrayBuilder()
				.add(jsonProvider.createObjectBuilder().add("type", "number"))
				.add(jsonProvider.createObjectBuilder().add("type", "boolean"))
				.add(jsonProvider.createObjectBuilder().add("type", "string"))
				.add(addRef(jsonProvider.createObjectBuilder(), YamlJsonSchemaHelper.DYNAMIC_EXPRESSION_DEF));

		JsonObjectBuilder properties = jsonProvider.createObjectBuilder()
				.add(".*", jsonProvider.createObjectBuilder().add("oneOf", oneOfArray));
		arrayItemDef.add("patternProperties", properties);
		arrayItemDef.add("additionalProperties", false);
		res.add("items", arrayItemDef);
		return res;
	}

	private JsonObjectBuilder createDynamicValueDef() {
		JsonObjectBuilder res = jsonProvider.createObjectBuilder();
		res.add("type", "object");
		JsonObjectBuilder properties = jsonProvider.createObjectBuilder();
		properties.add(YamlFields.DYN_VALUE_EXPRESSION_FIELD, jsonProvider.createObjectBuilder().add("type", "string"));
		res.add("properties", properties);
		res.add("additionalProperties", false);
		return res;
	}

	private JsonObjectBuilder createSmartDynamicValueDef(String smartValueType) {
		JsonObjectBuilder res = jsonProvider.createObjectBuilder();
		JsonArrayBuilder oneOfArray = jsonProvider.createArrayBuilder();
		oneOfArray.add(jsonProvider.createObjectBuilder().add("type", smartValueType));
		oneOfArray.add(addRef(jsonProvider.createObjectBuilder(), YamlJsonSchemaHelper.DYNAMIC_EXPRESSION_DEF));
		res.add("oneOf", oneOfArray);
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

			String dynamicValueType;
			if (!(dynamicValueClass instanceof Class)) {
				// for undefined type or for generic parameter types like "?"
				dynamicValueType = "object";
			} else {
				dynamicValueType = JsonInputConverter.resolveJsonPropertyType((Class<?>) dynamicValueClass);
			}
			switch (dynamicValueType){
				case "string":
					addRef(propertiesBuilder, SMART_DYNAMIC_VALUE_STRING_DEF);
					break;
				case "boolean":
					addRef(propertiesBuilder, SMART_DYNAMIC_VALUE_BOOLEAN_DEF);
					break;
				case "number":
					addRef(propertiesBuilder, SMART_DYNAMIC_VALUE_NUM_DEF);
					break;
				case "object":
					log.warn("Unknown dynamic value type for field " + field.getName());
					addRef(propertiesBuilder, SMART_DYNAMIC_VALUE_STRING_DEF);
					break;
				default:
					throw new IllegalArgumentException("Unsupported dynamic value type: " + dynamicValueType);
			}

		} else {
			throw new IllegalArgumentException("Unsupported dynamic value generic field " + genericType);
		}
	}

	/**
	 * Prepares the json schema for named entity (i.e. there is the top-level node with class name and all nested
	 * fields are written to nested fields)
	 *
	 * @param yamlName the top-level node name (entity name)
	 * @return the following structure:
	 * {
	 *   "type" : "object",
	 *   "properties" : {
	 *     "${yamlName}" : {
	 *       "type" : "object",
	 *       "properties" : {
	 *           fields extracted from class via reflection
	 *       },
	 *       "required": [...],
	 *       "additionalProperties": false
	 *     }
	 *   },
	 *   "additionalProperties" : false
	 * }
	 *
	 */
	public JsonObjectBuilder createNamedObjectImplDef(String yamlName, Class<?> clazz, JsonSchemaCreator jsonSchemaCreator, boolean additionalProperties) throws JsonSchemaPreparationException {
		JsonObjectBuilder res = jsonProvider.createObjectBuilder();
		res.add("type", "object");

		// on the top level there is a name only
		JsonObjectBuilder schemaBuilder = jsonProvider.createObjectBuilder();

		// other properties are located in nested object and automatically prepared via reflection
		JsonObjectBuilder propertiesBuilder = createJsonSchemaForClass(jsonSchemaCreator, clazz, additionalProperties);

		schemaBuilder.add(yamlName, propertiesBuilder);
		res.add("properties", schemaBuilder);
		res.add("additionalProperties", false);
		return res;
	}

	public static JsonObjectBuilder addRef(JsonObjectBuilder builder, String refValue){
		return builder.add("$ref", "#/$defs/" + refValue);
	}
}
