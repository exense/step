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
package step.plans.simple.schema;

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
import step.artefacts.CallFunction;
import step.artefacts.TokenSelector;
import step.core.Version;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.*;
import step.plans.nl.RootArtefactType;
import step.plans.simple.YamlPlanFields;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class SimplePlanJsonSchemaGenerator {

	private static final Logger log = LoggerFactory.getLogger(SimplePlanJsonSchemaGenerator.class);

	private static final String ARTEFACT_DEF = "ArtefactDef";
	private static final String ROOT_ARTEFACT_DEF = "RootArtefactDef";
	private static final String CALL_KEYWORD_FUNCTION_NAME_DEF = "CallKeywordFunctionNameDef";

	private final String targetPackage;

	private final Version actualVersion;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final JsonProvider jsonProvider = JsonProvider.provider();

	private final JsonSchemaCreator jsonSchemaCreator;
	private final SimpleDynamicValueJsonSchemaHelper dynamicValuesHelper = new SimpleDynamicValueJsonSchemaHelper(jsonProvider);

	public SimplePlanJsonSchemaGenerator(String targetPackage, Version actualVersion) {
		this.targetPackage = targetPackage;
		this.actualVersion = actualVersion;

		// TODO: further we can replace this hardcoded logic for some custom field metadata and processing with some enhanced solution (java annotations?)

		// --- Fields metadata rules (fields we want to rename)
		FieldMetadataExtractor fieldMetadataExtractor = new FieldMetadataExtractor() {

			private final FieldMetadataExtractor defaultMetadataExtractor = new DefaultFieldMetadataExtractor();

			@Override
			public FieldMetadata extractMetadata(Field field) {
				if (field.getDeclaringClass().equals(CallFunction.class) && field.getName().equals(YamlPlanFields.CALL_FUNCTION_ARGUMENT_ORIGINAL_FIELD)) {
					// rename 'argument' field to 'inputs'
					return new FieldMetadata(YamlPlanFields.CALL_FUNCTION_ARGUMENT_SIMPLE_FIELD, null, field.getType(), false);
				} else if (field.getDeclaringClass().equals(TokenSelector.class) && field.getName().equals(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD)) {
					// rename 'token' field to 'selectionCriteria'
					return new FieldMetadata(YamlPlanFields.TOKEN_SELECTOR_TOKEN_SIMPLE_FIELD, null, field.getType(), false);
				} else if(field.getDeclaringClass().equals(CallFunction.class) && field.getName().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD)){
					// rename 'function' field to 'keyword'
					return new FieldMetadata(YamlPlanFields.CALL_FUNCTION_FUNCTION_SIMPLE_FIELD, null, field.getType(), false);
				} else {
					return defaultMetadataExtractor.extractMetadata(field);
				}
			}
		};

		// --- Fields filtering rules
		JsonSchemaFieldProcessor commonFieldFilteringRule = new JsonSchemaFieldProcessor() {
			@Override
			public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) {
				// just skip these fields
				return field.isSynthetic()
						|| field.isAnnotationPresent(JsonIgnore.class)
						|| field.getType().equals(Object.class)
						|| Exception.class.isAssignableFrom(field.getType())
						|| Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers());
			}
		};

		JsonSchemaFieldProcessor artefactTechnicalFieldsFilteringRule = new JsonSchemaFieldProcessor() {
			@Override
			public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) {
				// ids and technical fields are skipped
				if (AbstractIdentifiableObject.class.equals(field.getDeclaringClass())) {
					return true;
				} else if (AbstractArtefact.class.equals(field.getDeclaringClass())) {
					// TODO: need to add some of these fields to JSON Schema (as optional fields)
					Set<String> technicalFields = Set.of(
							"dynamicName", "useDynamicName",
							"customAttributes", "persistNode",
							"instrumentNode", "attachments",
							"skipNode", "continueParentNodeExecutionOnError"
					);
					return technicalFields.contains(fieldMetadata.getFieldName());
				}
				return false;
			}
		};

		JsonSchemaFieldProcessor tokenInCallFunctionFilteringRule = new JsonSchemaFieldProcessor() {
			@Override
			public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) {
				// included inside CallKeywordFunctionNameDef
				return objectClass.equals(CallFunction.class) && field.getName().equals(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD);
			}
		};


		// --- Fields processing rules
		JsonSchemaFieldProcessor artefactChildrenProcessingRule = new JsonSchemaFieldProcessor() {
			@Override
			public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) throws JsonSchemaPreparationException {
				if(field.getDeclaringClass().equals(AbstractArtefact.class) && field.getName().equals("children")) {
					propertiesBuilder.add(fieldMetadata.getFieldName(),
							jsonProvider.createObjectBuilder()
									.add("type", "array")
									.add("items", addRef(jsonProvider.createObjectBuilder(), ARTEFACT_DEF))
					);
					return true;
				} else {
					return false;
				}
			}
		};

		JsonSchemaFieldProcessor artefactAttributesProcessingRules = new JsonSchemaFieldProcessor() {
			@Override
			public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) throws JsonSchemaPreparationException {
				if(field.getDeclaringClass().equals(AbstractOrganizableObject.class) && field.getName().equals("attributes")) {
					// use artefact name as default
					propertiesBuilder.add(YamlPlanFields.NAME_SIMPLE_FIELD, jsonProvider.createObjectBuilder().add("type", "string").add("default", getArtefactName(objectClass)));
					return true;
				} else {
					return false;
				}
			}
		};

		JsonSchemaFieldProcessor keywordNameProcessingRule = new JsonSchemaFieldProcessor() {
			@Override
			public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) {
				if (objectClass.equals(CallFunction.class) && field.getName().equals(YamlPlanFields.CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD)) {
					JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
					SimplePlanJsonSchemaGenerator.addRef(nestedPropertyParamsBuilder, CALL_KEYWORD_FUNCTION_NAME_DEF);
					propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
					return true;
				}
				return false;
			}
		};

		JsonSchemaFieldProcessor keywordInputsFieldProcessingRule = new JsonSchemaFieldProcessor() {
			@Override
			public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) {
				if (objectClass.equals(CallFunction.class) && field.getName().equals(YamlPlanFields.CALL_FUNCTION_ARGUMENT_ORIGINAL_FIELD)) {
					JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
					SimplePlanJsonSchemaGenerator.addRef(nestedPropertyParamsBuilder, SimpleDynamicValueJsonSchemaHelper.DYNAMIC_KEYWORD_INPUTS_DEF);
					propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
					return true;
				}
				return false;
			}
		};

		JsonSchemaFieldProcessor tokenFieldProcessingRule = new JsonSchemaFieldProcessor() {
			@Override
			public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) {
				if (TokenSelector.class.isAssignableFrom(objectClass) && field.getName().equals(YamlPlanFields.TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD)) {
					JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();
					SimplePlanJsonSchemaGenerator.addRef(nestedPropertyParamsBuilder, SimpleDynamicValueJsonSchemaHelper.DYNAMIC_KEYWORD_INPUTS_DEF);
					propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
					return true;
				}
				return false;
			}
		};

		JsonSchemaFieldProcessor enumProcessingRule = new JsonSchemaFieldProcessor() {
			@Override
			public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) {
				if (field.getType().isEnum()) {
					JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();

					JsonArrayBuilder enumArray = jsonProvider.createArrayBuilder();
					for (Object enumValue : field.getType().getEnumConstants()) {
						enumArray.add(enumValue.toString());
					}
					nestedPropertyParamsBuilder.add("enum", enumArray);

					propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
					return true;
				}
				return false;
			}
		};

		JsonSchemaFieldProcessor dynamicValueProcessingRule = new JsonSchemaFieldProcessor() {
			@Override
			public boolean applyCustomProcessing(Class<?> objectClass, Field field, FieldMetadata fieldMetadata, JsonObjectBuilder propertiesBuilder, List<String> requiredPropertiesOutput) {
				if (DynamicValue.class.isAssignableFrom(field.getType())) {
					JsonObjectBuilder nestedPropertyParamsBuilder = jsonProvider.createObjectBuilder();

					dynamicValuesHelper.applyDynamicValueDefForField(field, nestedPropertyParamsBuilder);

					propertiesBuilder.add(fieldMetadata.getFieldName(), nestedPropertyParamsBuilder);
					return true;
				}
				return false;
			}
		};

		List<JsonSchemaFieldProcessor> processingRules = List.of(
				commonFieldFilteringRule,
				artefactTechnicalFieldsFilteringRule,
				tokenInCallFunctionFilteringRule,
				artefactChildrenProcessingRule,
				artefactAttributesProcessingRules,
				keywordNameProcessingRule,
				keywordInputsFieldProcessingRule,
				tokenFieldProcessingRule,
				enumProcessingRule,
				dynamicValueProcessingRule
		);

		this.jsonSchemaCreator = new JsonSchemaCreator(jsonProvider, new AggregatedJsonSchemaFieldProcessor(processingRules), fieldMetadataExtractor);
	}

	public JsonNode generateJsonSchema() throws JsonSchemaPreparationException {
		JsonObjectBuilder topLevelBuilder = jsonProvider.createObjectBuilder();

		// common fields for json schema
		topLevelBuilder.add("$schema", "http://json-schema.org/draft-07/schema#");
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
		// plan only has "name", "version", and the root artifact
		JsonObjectBuilder objectBuilder = jsonProvider.createObjectBuilder();
		objectBuilder.add("name", jsonProvider.createObjectBuilder().add("type", "string"));
		objectBuilder.add("version", jsonProvider.createObjectBuilder().add("type", "string").add("default", actualVersion.toString()));
		objectBuilder.add("root", addRef(jsonProvider.createObjectBuilder(), ROOT_ARTEFACT_DEF));
		return objectBuilder;
	}

	static JsonObjectBuilder addRef(JsonObjectBuilder builder, String refValue){
		return builder.add("$ref", "#/$defs/" + refValue);
	}

	/**
	 * Prepares definitions to be reused in json subschemas
	 */
	private JsonObjectBuilder createDefs() throws JsonSchemaPreparationException {
		JsonObjectBuilder defsBuilder = jsonProvider.createObjectBuilder();

		// prepare definitions for generic DynamicValue class
		Map<String, JsonObjectBuilder> dynamicValueDefs = dynamicValuesHelper.createDynamicValueImplDefs();
		for (Map.Entry<String, JsonObjectBuilder> dynamicValueDef : dynamicValueDefs.entrySet()) {
			defsBuilder.add(dynamicValueDef.getKey(), dynamicValueDef.getValue());
		}

		// simplified keyword name for 'CallKeyword' artefact
		defsBuilder.add(CALL_KEYWORD_FUNCTION_NAME_DEF, createCallKeywordFunctionNameDef());

		// prepare definitions for subclasses annotated with @Artefact
		ArtefactDefinitions artefactImplDefs = createArtefactImplDefs();
		for (Map.Entry<String, JsonObjectBuilder> artefactImplDef : artefactImplDefs.allArtefactDefs.entrySet()) {
			defsBuilder.add(artefactImplDef.getKey(), artefactImplDef.getValue());
		}

		// add definition for "anyOf" artefact definitions prepared above
		defsBuilder.add(ARTEFACT_DEF, createArtefactDef(artefactImplDefs.allArtefactDefs.keySet()));
		defsBuilder.add(ROOT_ARTEFACT_DEF, createArtefactDef(artefactImplDefs.rootArtefactDefs));
		return defsBuilder;
	}

	/**
	 * "oneOf": [
	 *  {
	 *    "type": "object",
	 *    "properties": {
	 *      "name": {
	 *        "$ref": "#/$defs/SmartDynamicValueStringDef"
	 *      }
	 *    },
	 *    "additionalProperties": false
	 *  },
	 *  {
	 *    "type": "object",
	 *    "properties": {
	 *      "selectionCriteria": {
	 *        "$ref" : "#/$defs/DynamicKeywordInputsDef"
	 *      }
	 *    },
	 *    "additionalProperties": false
	 *  }
	 *]
	 */
	private JsonObjectBuilder createCallKeywordFunctionNameDef() {
		JsonObjectBuilder result = jsonProvider.createObjectBuilder();

		JsonObjectBuilder keywordFunctionName = addRef(jsonProvider.createObjectBuilder(), SimpleDynamicValueJsonSchemaHelper.SMART_DYNAMIC_VALUE_STRING_DEF);

		JsonObjectBuilder keywordSelectionCriteria = jsonProvider.createObjectBuilder();
		keywordSelectionCriteria.add("type", "object");
		keywordSelectionCriteria.add("properties", jsonProvider.createObjectBuilder()
				.add("selectionCriteria", addRef(jsonProvider.createObjectBuilder(), SimpleDynamicValueJsonSchemaHelper.DYNAMIC_KEYWORD_INPUTS_DEF))
		);
		keywordSelectionCriteria.add("additionalProperties", false);

		result.add("oneOf", jsonProvider.createArrayBuilder().add(keywordFunctionName).add(keywordSelectionCriteria));
		return result;
	}

	private ArtefactDefinitions createArtefactImplDefs() throws JsonSchemaPreparationException {
		ArtefactDefinitions artefactDefinitions = new ArtefactDefinitions();

		// scan all @Artefact classes in classpath and automatically prepare definitions for them
		Reflections r = new Reflections( new ConfigurationBuilder().forPackage(targetPackage));

		// exclude artefacts from test packages
		List<Class<?>> artefactClasses = r.getTypesAnnotatedWith(Artefact.class)
				.stream()
				.filter(c -> !c.getAnnotation(Artefact.class).test())
				.collect(Collectors.toList());
		log.info("The following {} artefact classes detected: {}", artefactClasses.size(), artefactClasses);

		for (Class<?> artefactClass : artefactClasses) {
			// use the name of artefact as definition name
			String name = getArtefactName(artefactClass);
			String defName = name + "Def";

			// scan all fields in artefact class and put them to artefact definition
			JsonObjectBuilder def = createArtefactImplDef(name, artefactClass);
			artefactDefinitions.allArtefactDefs.put(defName, def);

			// for root artefacts we only support the subset of all artefact definitions
			for (RootArtefactType rootArtefactType : RootArtefactType.values()) {
				if(rootArtefactType.createRootArtefact().getClass().equals(artefactClass)){
					artefactDefinitions.rootArtefactDefs.add(defName);
				}
			}
		}
		return artefactDefinitions;
	}

	private static String getArtefactName(Class<?> artefactClass) {
		Artefact ann = artefactClass.getAnnotation(Artefact.class);
		return ann.name() == null || ann.name().isEmpty() ? artefactClass.getSimpleName() : ann.name();
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
		res.add("additionalProperties", false);
		return res;
	}

	private void fillArtefactProperties(Class<?> artefactClass, JsonObjectBuilder artefactProperties, String artefactName) throws JsonSchemaPreparationException {
		log.info("Preparing json schema for artefact class {}...", artefactClass);

		// analyze hierarchy of class annotated with @Artefact
		List<Field> allFieldsInArtefactHierarchy = new ArrayList<>();
		Class<?> currentClass = artefactClass;
		while (currentClass != null) {
			allFieldsInArtefactHierarchy.addAll(List.of(currentClass.getDeclaredFields()));
			currentClass = currentClass.getSuperclass();
		}

		// for each field we want either build the json schema via reflection
		// or use some predefined schemas for some special classes (like step.core.dynamicbeans.DynamicValue)
		try {
			Collections.reverse(allFieldsInArtefactHierarchy);
			jsonSchemaCreator.processFields(artefactClass, artefactProperties, allFieldsInArtefactHierarchy, new ArrayList<>());
		} catch (Exception ex) {
			throw new JsonSchemaPreparationException("Unable to process artefact " + artefactName, ex);
		}
	}

	private JsonObjectBuilder createArtefactDef(Collection<String> artefactImplReferences) {
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		builder.add("type", "object");
		JsonArrayBuilder arrayBuilder = jsonProvider.createArrayBuilder();
		for (String artefactImplReference : artefactImplReferences) {
			arrayBuilder.add(addRef(jsonProvider.createObjectBuilder(), artefactImplReference));
		}
		builder.add("oneOf", arrayBuilder);
		return builder;
	}

	private JsonNode fromJakartaToJsonNode(JsonObjectBuilder objectBuilder) throws JsonProcessingException {
		return objectMapper.readTree(objectBuilder.build().toString());
	}

	private static class ArtefactDefinitions {
		private final Map<String, JsonObjectBuilder> allArtefactDefs = new HashMap<>();
		private final Collection<String> rootArtefactDefs = new ArrayList<>();
	}
}
