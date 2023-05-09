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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.JsonInputConverter;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.handlers.javahandler.jsonschema.KeywordJsonSchemaCreator;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class SimplifiedPlanJsonSchemaGenerator {

	private static final Logger log = LoggerFactory.getLogger(SimplifiedPlanJsonSchemaGenerator.class);

	private static final String DYNAMIC_VALUE_STRING_DEF = "DynamicValueStringDef";
	private static final String DYNAMIC_VALUE_NUM_DEF = "DynamicValueNumDef";
	private static final String DYNAMIC_VALUE_BOOLEAN_DEF = "DynamicValueBooleanDef";
	private static final String ARTEFACT_DEF = "ArtefactDef";

	private final String targetPackage;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final JsonProvider jsonProvider = JsonProvider.provider();
	private final KeywordJsonSchemaCreator jsonSchemaCreator = new KeywordJsonSchemaCreator();

	public SimplifiedPlanJsonSchemaGenerator(String targetPackage) {
		this.targetPackage = targetPackage;
	}

	public JsonNode generateJsonSchema() throws JsonSchemaPreparationException {
		JsonObjectBuilder topLevelBuilder = jsonProvider.createObjectBuilder();

		// common fields for json schema
		topLevelBuilder.add("$schema", "https://json-schema.org/draft/2020-12/schema");
		topLevelBuilder.add("title", "Plan");
		topLevelBuilder.add("type", "object");

		// prepare definitions to be reused in subschemas (referenced via $ref property)
		topLevelBuilder.add("$defs", createDefs());

		// add properties for top-level "plan" object
		topLevelBuilder.add("properties", createPlanProperties());
		topLevelBuilder.add("required", jsonProvider.createArrayBuilder().add("name").add("root"));
		topLevelBuilder.add( "additionalProperties", false);

		// convert jakarta objects to jackson JsonNode
		try {
			return fromJakartaToJsonNode(topLevelBuilder);
		} catch (JsonProcessingException e) {
			throw new JsonSchemaPreparationException("Unable to convert json to jackson jsonNode", e);
		}
	}

	private JsonObjectBuilder createPlanProperties() {
		// plan only has "name" and the root artifact
		JsonObjectBuilder objectBuilder = jsonProvider.createObjectBuilder();
		objectBuilder.add("name", jsonProvider.createObjectBuilder().add("type", "string"));
		objectBuilder.add("root", addRef(jsonProvider.createObjectBuilder(), ARTEFACT_DEF));
		return objectBuilder;
	}

	private JsonObjectBuilder addRef(JsonObjectBuilder builder, String refValue){
		return builder.add("$ref", "#/$defs/" + refValue);
	}

	private JsonObjectBuilder createDefs() throws JsonSchemaPreparationException {
		JsonObjectBuilder defsBuilder = jsonProvider.createObjectBuilder();

		// prepare definitions for generic DynamicValue class
		Map<String, JsonObjectBuilder> dynamicValueDefs = createDynamicValueImplDefs();
		for (Map.Entry<String, JsonObjectBuilder> dynamicValueDef : dynamicValueDefs.entrySet()) {
			defsBuilder.add(dynamicValueDef.getKey(), dynamicValueDef.getValue());
		}

		// prepare definitions for subclasses annotated with @Artefact
		Map<String, JsonObjectBuilder> artefactImplDefs = createArtefactImplDefs();
		for (Map.Entry<String, JsonObjectBuilder> artefactImplDef : artefactImplDefs.entrySet()) {
			defsBuilder.add(artefactImplDef.getKey(), artefactImplDef.getValue());
		}

		// add definition for "anyOf" artefact definitions prepared above
		defsBuilder.add(ARTEFACT_DEF, createArtefactDef(artefactImplDefs.keySet()));
		return defsBuilder;
	}

	private Map<String, JsonObjectBuilder> createArtefactImplDefs() throws JsonSchemaPreparationException {
		// scan all @Artefact classes in classpath and automatically prepare definitions for them
		Map<String, JsonObjectBuilder> res = new HashMap<>();
		Reflections r = new Reflections( new ConfigurationBuilder().forPackage(targetPackage));

		// exclude artefacts from test packages
		List<Class<?>> artefactClasses = r.getTypesAnnotatedWith(Artefact.class)
				.stream()
				.filter(c -> !c.getAnnotation(Artefact.class).test())
				.collect(Collectors.toList());
		log.info("The following {} artefact classes detected: {}", artefactClasses.size(), artefactClasses);

		for (Class<?> artefactClass : artefactClasses) {
			// use the name of artefact as definition name
			Artefact ann = artefactClass.getAnnotation(Artefact.class);
			String name = ann.name() == null || ann.name().isEmpty() ? artefactClass.getSimpleName() : ann.name();
			String defName = name + "Def";

			// scan all fields in artefact class and put them to artefact definition
			res.put(defName, createArtefactImplDef(name, artefactClass));
		}
		return res;
	}

	private JsonObjectBuilder createArtefactImplDef(String name, Class<?> artefactClass) throws JsonSchemaPreparationException {
		JsonObjectBuilder res = jsonProvider.createObjectBuilder();
		res.add("type", "object");

		// artefact has the top-level property matching the artefact name
		JsonObjectBuilder artefactNameProperty = jsonProvider.createObjectBuilder();

		// other properties are located in nested object and automatically prepared via reflection
		JsonObjectBuilder artefactProperties = jsonProvider.createObjectBuilder();
		fillArtefactProperties(artefactClass, artefactProperties, name);

		artefactNameProperty.add(name, jsonProvider.createObjectBuilder().add("type", "object").add("properties", artefactProperties));
		res.add("properties", artefactNameProperty);
		return res;
	}

	private void fillArtefactProperties(Class<?> artefactClass, JsonObjectBuilder artefactProperties, String artefactName) throws JsonSchemaPreparationException {
		log.info("Preparing json schema for artefact class {}...", artefactClass);

		// analyze hierarchy of class annotated with @Artefact
		List<Class<?>> classHierarchy = new ArrayList<>();
		Class<?> currentClass = artefactClass;
		while (currentClass != null) {
			classHierarchy.add(currentClass);
			currentClass = currentClass.getSuperclass();
		}

		for (Class<?> c : classHierarchy) {
			// for some parent classes we want to avoid auto generation via reflection (for instance, for AbstractArtefact we want to skip the most of technical fields)
			if (!applyCustomPropertiesGenerationForClass(c, artefactProperties, artefactName)) {
				// the common (not custom logic for some Artefact class

				// get all from fields from the certain class
				Field[] fields = c.getDeclaredFields();

				// for each field we want either build the json schema via reflection
				// or use some predefined schemas for some special classes (like step.core.dynamicbeans.DynamicValue)
				jsonSchemaCreator.processFields(
						new KeywordJsonSchemaCreator.FieldPropertyProcessor() {
							@Override
							public boolean applyCustomProcessing(Field field, JsonObjectBuilder propertiesBuilder) {
								if(field.getType().isEnum()){
									JsonArrayBuilder enumArray = jsonProvider.createArrayBuilder();
									for (Object enumValue : field.getType().getEnumConstants()) {
										enumArray.add(enumValue.toString());
									}
									propertiesBuilder.add("enum", enumArray);
									return true;
								} else if (DynamicValue.class.isAssignableFrom(field.getType())) {
									// for dynamic value we need to reference the definition prepared above
									// the definition depends on generic type used in dynamic value (string, integer, boolean, etc)
									Type genericType = field.getGenericType();
									if (genericType instanceof ParameterizedType) {
										Type[] arguments = ((ParameterizedType) genericType).getActualTypeArguments();
										Type dynamicValueClass = arguments[0];
										if (!(dynamicValueClass instanceof Class)) {
											throw new IllegalArgumentException(artefactClass +  ": Unsupported dynamic value type " + dynamicValueClass);
										}
										String dynamicValueType = JsonInputConverter.resolveJsonPropertyType((Class<?>) dynamicValueClass);
										switch (dynamicValueType){
											case "string":
												addRef(propertiesBuilder, DYNAMIC_VALUE_STRING_DEF);
												break;
											case "boolean":
												addRef(propertiesBuilder, DYNAMIC_VALUE_BOOLEAN_DEF);
												break;
											case "number":
												addRef(propertiesBuilder, DYNAMIC_VALUE_NUM_DEF);
												break;
											case "object":
												log.warn(artefactClass + ": Unknown dynamic value type for field " + field.getName());
												addRef(propertiesBuilder, DYNAMIC_VALUE_STRING_DEF);
												break;
											default:
												throw new IllegalArgumentException(artefactClass + ": Unsupported dynamic value type: " + dynamicValueType);
										}

									} else {
										throw new IllegalArgumentException(artefactClass + ": Unsupported dynamic value generic field " + genericType);
									}
									return true;
								}
								return false;
							}

							@Override
							public boolean skipField(Field field) {
								if(field.isSynthetic()){
									return true;
								} else if (field.isAnnotationPresent(JsonIgnore.class)) {
									// just skip all fields with JsonIgnore
									return true;
								} else if (field.getType().equals(Object.class)) {
									return true;
								} else if (Exception.class.isAssignableFrom(field.getType())) {
									return true;
								}
								return false;
							}
						},
						artefactProperties,
						new ArrayList<>(),
						Arrays.stream(fields).collect(Collectors.toList())
				);
			} else {
				break;
			}
		}
	}

	private boolean applyCustomPropertiesGenerationForClass(Class<?> c, JsonObjectBuilder artefactProperties, String artefactName) {
		if (c.equals(AbstractArtefact.class)) {
			artefactProperties.add("description", jsonProvider.createObjectBuilder().add("type", "string"));
			// use artefact name as default
			artefactProperties.add("name", jsonProvider.createObjectBuilder().add("type", "string").add("default", artefactName));
			artefactProperties.add("children",
					jsonProvider.createObjectBuilder()
							.add("type", "array")
							.add("items", addRef(jsonProvider.createObjectBuilder(), ARTEFACT_DEF))
			);

			return true;
		}
		return false;
	}

	private Map<String, JsonObjectBuilder> createDynamicValueImplDefs() {
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
		properties.add("expressionType", jsonProvider.createObjectBuilder().add("type", "string"));
		properties.add("value", jsonProvider.createObjectBuilder().add("type", valueType));
		res.add("properties", properties);
		return res;
	}

	private JsonObjectBuilder createArtefactDef(Collection<String> artefactImplReferences) {
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		builder.add("type", "object");
		JsonArrayBuilder arrayBuilder = jsonProvider.createArrayBuilder();
		for (String artefactImplReference : artefactImplReferences) {
			arrayBuilder.add(addRef(jsonProvider.createObjectBuilder(), artefactImplReference));
		}
		builder.add("anyOf", arrayBuilder);
		return builder;
	}

	private JsonNode fromJakartaToJsonNode(JsonObjectBuilder objectBuilder) throws JsonProcessingException {
		return objectMapper.readTree(objectBuilder.build().toString());
	}

}
