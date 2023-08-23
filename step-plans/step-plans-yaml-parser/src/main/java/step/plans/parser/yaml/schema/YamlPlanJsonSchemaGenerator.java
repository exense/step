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
package step.plans.parser.yaml.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Version;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.scanner.AnnotationScanner;
import step.core.scanner.CachedAnnotationScanner;
import step.handlers.javahandler.jsonschema.FieldMetadataExtractor;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.plans.nl.RootArtefactType;
import step.plans.parser.yaml.YamlPlanFields;
import step.plans.parser.yaml.rules.*;
import step.plans.parser.yaml.ArtefactFieldMetadataExtractor;
import step.plans.parser.yaml.YamlPlanReaderExtender;
import step.plans.parser.yaml.YamlPlanReaderExtension;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static step.core.scanner.Classes.newInstanceAs;
import static step.plans.parser.yaml.YamlPlanFields.ARTEFACT_CHILDREN;

public class YamlPlanJsonSchemaGenerator {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanJsonSchemaGenerator.class);

	public static final String ARTEFACT_DEF = "ArtefactDef";
	private static final String ROOT_ARTEFACT_DEF = "RootArtefactDef";

	protected final String targetPackage;

	protected final Version actualVersion;

	protected final ObjectMapper objectMapper = new ObjectMapper();
	protected final JsonProvider jsonProvider = JsonProvider.provider();

	protected final JsonSchemaCreator jsonSchemaCreator;
	protected final YamlDynamicValueJsonSchemaHelper dynamicValuesHelper = new YamlDynamicValueJsonSchemaHelper(jsonProvider);

	public YamlPlanJsonSchemaGenerator(String targetPackage, Version actualVersion) {
		this.targetPackage = targetPackage;
		this.actualVersion = actualVersion;

		// TODO: further we can replace this hardcoded logic for some custom field metadata and processing with some enhanced solution (java annotations?)

		// --- Fields metadata rules (fields we want to rename)
		FieldMetadataExtractor fieldMetadataExtractor = prepareMetadataExtractor();

		List<JsonSchemaFieldProcessor> processingRules = prepareFieldProcessors();
		this.jsonSchemaCreator = new JsonSchemaCreator(jsonProvider, new AggregatedJsonSchemaFieldProcessor(processingRules), fieldMetadataExtractor);
	}

	protected List<JsonSchemaFieldProcessor> prepareFieldProcessors() {
		List<JsonSchemaFieldProcessor> result = new ArrayList<>();

		// -- BASIC PROCESSING RULES
		result.add(new CommonFilteredFieldRule().getJsonSchemaFieldProcessor(jsonProvider));
		result.add(new TechnicalFieldRule().getJsonSchemaFieldProcessor(jsonProvider));

		JsonSchemaFieldProcessor artefactChildrenProcessingRule = (objectClass, field, fieldMetadata, propertiesBuilder, requiredPropertiesOutput) -> {
			if(field.getDeclaringClass().equals(AbstractArtefact.class) && field.getName().equals(ARTEFACT_CHILDREN)) {
				propertiesBuilder.add(fieldMetadata.getFieldName(),
						jsonProvider.createObjectBuilder()
								.add("type", "array")
								.add("items", addRef(jsonProvider.createObjectBuilder(), ARTEFACT_DEF))
				);
				return true;
			} else {
				return false;
			}
		};
		result.add(artefactChildrenProcessingRule);

		// -- RULES FROM EXTENSIONS HAVE LESS PRIORITY THAN BASIC RULES, BUT MORE PRIORITY THAN OTHER RULES
		result.addAll(getFieldExtensions());

		// -- RULES FOR OS ARTEFACTS
		result.add(new NodeNameRule().getJsonSchemaFieldProcessor(jsonProvider));
		result.add(new KeywordSelectionRule().getJsonSchemaFieldProcessor(jsonProvider));
		result.add(new KeywordRoutingRule().getJsonSchemaFieldProcessor(jsonProvider));
		result.add(new KeywordInputsRule().getJsonSchemaFieldProcessor(jsonProvider));
		result.add(new FunctionGroupSelectionRule().getJsonSchemaFieldProcessor(jsonProvider));
		result.add(new CheckExpressionRule().getJsonSchemaFieldProcessor(jsonProvider));

		// -- SOME DEFAULT RULES FOR ENUMS AND DYNAMIC FIELDS
		result.add(new DynamicFieldRule().getJsonSchemaFieldProcessor(jsonProvider));
		result.add(new EnumFieldRule().getJsonSchemaFieldProcessor(jsonProvider));

		return result;
	}

	protected List<JsonSchemaFieldProcessor> getFieldExtensions() {
		List<JsonSchemaFieldProcessor> extensions = new ArrayList<>();
		CachedAnnotationScanner.getClassesWithAnnotation(YamlPlanReaderExtension.class).stream()
				.map(newInstanceAs(YamlPlanReaderExtender.class)).forEach(e -> extensions.addAll(e.getJsonSchemaFieldProcessingExtensions()));
		return extensions;
	}

	protected List<YamlPlanJsonSchemaDefinitionCreator> getDefinitionsExtensions() {
		List<YamlPlanJsonSchemaDefinitionCreator> extensions = new ArrayList<>();
		CachedAnnotationScanner.getClassesWithAnnotation(YamlPlanReaderExtension.class).stream()
				.map(newInstanceAs(YamlPlanReaderExtender.class)).forEach(e -> extensions.addAll(e.getJsonSchemaDefinitionsExtensions()));
		return extensions;
	}

	protected ArtefactFieldMetadataExtractor prepareMetadataExtractor() {
		return new ArtefactFieldMetadataExtractor();
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
		// in 'version' we should either explicitly specify the current json schema version or skip this field
		objectBuilder.add("version", jsonProvider.createObjectBuilder().add("const", actualVersion.toString()));
		objectBuilder.add("root", addRef(jsonProvider.createObjectBuilder(), ROOT_ARTEFACT_DEF));
		return objectBuilder;
	}

	public static JsonObjectBuilder addRef(JsonObjectBuilder builder, String refValue){
		return builder.add("$ref", "#/$defs/" + refValue);
	}

	/**
	 * Prepares definitions to be reused in json subschemas
	 */
	private JsonObjectBuilder createDefs() throws JsonSchemaPreparationException {
		JsonObjectBuilder defsBuilder = jsonProvider.createObjectBuilder();

		List<YamlPlanJsonSchemaDefinitionCreator> definitionCreators = new ArrayList<>();

		// prepare definitions for generic DynamicValue class
		definitionCreators.add((defsList) -> {
			Map<String, JsonObjectBuilder> dynamicValueDefs = dynamicValuesHelper.createDynamicValueImplDefs();
			for (Map.Entry<String, JsonObjectBuilder> dynamicValueDef : dynamicValueDefs.entrySet()) {
				defsBuilder.add(dynamicValueDef.getKey(), dynamicValueDef.getValue());
			}
		});

		// prepare definitions for subclasses annotated with @Artefact
		definitionCreators.add((defsList) -> {
			ArtefactDefinitions artefactImplDefs = createArtefactImplDefs();
			for (Map.Entry<String, JsonObjectBuilder> artefactImplDef : artefactImplDefs.allArtefactDefs.entrySet()) {
				defsBuilder.add(artefactImplDef.getKey(), artefactImplDef.getValue());
			}

			// add definition for "anyOf" artefact definitions prepared above
			defsBuilder.add(ARTEFACT_DEF, createArtefactDef(artefactImplDefs.allArtefactDefs.keySet()));
			defsBuilder.add(ROOT_ARTEFACT_DEF, createArtefactDef(artefactImplDefs.rootArtefactDefs));
		});

		// add definitions from extensions (additional definitions for EE artefacts)
		definitionCreators.addAll(getDefinitionsExtensions());

		for (YamlPlanJsonSchemaDefinitionCreator definitionCreator : definitionCreators) {
			definitionCreator.addDefinition(defsBuilder);
		}

		return defsBuilder;
	}

	private ArtefactDefinitions createArtefactImplDefs() throws JsonSchemaPreparationException {
		try (AnnotationScanner annotationScanner = AnnotationScanner.forAllClassesFromContextClassLoader()) {
			ArtefactDefinitions artefactDefinitions = new ArtefactDefinitions();

			// scan all @Artefact classes in classpath and automatically prepare definitions for them
			List<Class<?>> artefactClasses = annotationScanner.getClassesWithAnnotation(Artefact.class)
					.stream()
					.filter(c -> !c.getAnnotation(Artefact.class).test()) // exclude test artefacts
					.sorted(Comparator.comparing(Class::getSimpleName))
					.collect(Collectors.toList());
			log.info("The following {} artefact classes detected: {}", artefactClasses.size(), artefactClasses);

			for (Class<?> artefactClass : artefactClasses) {
				// use the name of artefact as definition name
				String name = AbstractArtefact.getArtefactName((Class<? extends AbstractArtefact>) artefactClass);
				String defName = name + "Def";

				// scan all fields in artefact class and put them to artefact definition
				JsonObjectBuilder def = createArtefactImplDef(name, artefactClass);
				artefactDefinitions.allArtefactDefs.put(defName, def);

				// for root artefacts we only support the subset of all artefact definitions
				for (RootArtefactType rootArtefactType : RootArtefactType.values()) {
					if (rootArtefactType.createRootArtefact().getClass().equals(artefactClass)) {
						artefactDefinitions.rootArtefactDefs.add(defName);
					}
				}
			}
			return artefactDefinitions;
		}
	}

	private JsonObjectBuilder createArtefactImplDef(String name, Class<?> artefactClass) throws JsonSchemaPreparationException {
		JsonObjectBuilder res = jsonProvider.createObjectBuilder();
		res.add("type", "object");

		// artefact has the top-level property matching the artefact name
		JsonObjectBuilder artefactNameProperty = jsonProvider.createObjectBuilder();

		// other properties are located in nested object and automatically prepared via reflection
		JsonObjectBuilder artefactProperties = jsonProvider.createObjectBuilder();
		fillArtefactProperties(artefactClass, artefactProperties, name);

		// use camelCase for artefact names in yaml
		artefactNameProperty.add(YamlPlanFields.javaArtefactNameToYaml(name), jsonProvider.createObjectBuilder().add("type", "object").add("properties", artefactProperties));
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
		Collections.reverse(allFieldsInArtefactHierarchy);

		// for each field we want either build the json schema via reflection
		// or use some predefined schemas for some special classes (like step.core.dynamicbeans.DynamicValue)
		try {
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
