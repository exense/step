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
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.scanner.CachedAnnotationScanner;
import step.core.yaml.schema.*;
import step.handlers.javahandler.jsonschema.*;
import step.plans.parser.yaml.YamlArtefactsLookuper;
import step.plans.parser.yaml.YamlPlanReaderExtender;
import step.plans.parser.yaml.YamlPlanReaderExtension;
import step.plans.parser.yaml.model.AbstractYamlArtefact;

import java.util.*;
import java.util.stream.Collectors;

import static step.core.scanner.Classes.newInstanceAs;

public class YamlPlanJsonSchemaGenerator {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanJsonSchemaGenerator.class);

	public static final String ARTEFACT_DEF = "ArtefactDef";
	private static final String ROOT_ARTEFACT_DEF = "RootArtefactDef";

	protected final String targetPackage;

	protected final Version actualVersion;
	private final String schemaId;

	protected final ObjectMapper objectMapper = DefaultJacksonMapperProvider.getObjectMapper();
	protected final JsonProvider jsonProvider = JsonProvider.provider();

	protected final JsonSchemaCreator jsonSchemaCreator;
	protected final YamlJsonSchemaHelper schemaHelper = new YamlJsonSchemaHelper(jsonProvider);
	protected final YamlResourceReferenceJsonSchemaHelper resourceReferenceJsonSchemaHelper = new YamlResourceReferenceJsonSchemaHelper(jsonProvider);

	public YamlPlanJsonSchemaGenerator(String targetPackage, Version actualVersion, String schemaId) {
		this.targetPackage = targetPackage;
		this.actualVersion = actualVersion;
		this.schemaId = schemaId;

		// TODO: further we can replace this hardcoded logic for some custom field metadata and processing with some enhanced solution (java annotations?)

		// --- Fields metadata rules (fields we want to rename)
		FieldMetadataExtractor fieldMetadataExtractor = prepareMetadataExtractor();

		List<JsonSchemaFieldProcessor> processingRules = prepareFieldProcessors();
		this.jsonSchemaCreator = new JsonSchemaCreator(jsonProvider, new AggregatedJsonSchemaFieldProcessor(processingRules), fieldMetadataExtractor);
	}

	protected List<JsonSchemaFieldProcessor> prepareFieldProcessors() {
		List<JsonSchemaFieldProcessor> result = new ArrayList<>();

		// -- BASIC PROCESSING RULES
		result.add(new CommonFilteredFieldProcessor());

		// -- SOME DEFAULT RULES FOR ENUMS AND DYNAMIC FIELDS
		result.add(new DynamicValueFieldProcessor(jsonProvider));
		result.add(new EnumFieldProcessor(jsonProvider));

		return result;
	}

	protected List<JsonSchemaDefinitionCreator> getDefinitionsExtensions() {
		List<JsonSchemaDefinitionCreator> extensions = new ArrayList<>();
		CachedAnnotationScanner.getClassesWithAnnotation(YamlPlanReaderExtension.LOCATION, YamlPlanReaderExtension.class, Thread.currentThread().getContextClassLoader()).stream()
				.map(newInstanceAs(YamlPlanReaderExtender.class)).forEach(e -> extensions.addAll(e.getJsonSchemaDefinitionsExtensions()));
		return extensions;
	}

	// TODO: replace with annotations
	public static FieldMetadataExtractor prepareMetadataExtractor() {
		List<FieldMetadataExtractor> metadataExtractors = new ArrayList<>();

		CachedAnnotationScanner.getClassesWithAnnotation(YamlPlanReaderExtension.LOCATION, YamlPlanReaderExtension.class, Thread.currentThread().getContextClassLoader()).stream()
				.map(newInstanceAs(YamlPlanReaderExtender.class)).forEach(e -> metadataExtractors.addAll(e.getMetadataExtractorExtensions()));
		metadataExtractors.add(new DefaultFieldMetadataExtractor());

		return new AggregatingFieldMetadataExtractor(metadataExtractors);
	}

	public JsonNode generateJsonSchema() throws JsonSchemaPreparationException {
		JsonObjectBuilder topLevelBuilder = jsonProvider.createObjectBuilder();

		// common fields for json schema
		topLevelBuilder.add("$schema", "http://json-schema.org/draft-07/schema#");
		if (this.schemaId != null) {
			topLevelBuilder.add("$id", this.schemaId);
		}
		topLevelBuilder.add("title", "Plan");
		topLevelBuilder.add("type", "object");

		// prepare definitions to be reused in subschemas (referenced via $ref property)
		topLevelBuilder.add("$defs", createDefs());

		// add properties for top-level "plan" object
		topLevelBuilder.add("properties", createYamlPlanProperties(true));
		topLevelBuilder.add("required", jsonProvider.createArrayBuilder().add("name").add("root"));
		topLevelBuilder.add( "additionalProperties", false);

		// convert jakarta objects to jackson JsonNode
		try {
			return fromJakartaToJsonNode(topLevelBuilder);
		} catch (JsonProcessingException e) {
			throw new JsonSchemaPreparationException("Unable to convert json to jackson jsonNode", e);
		}
	}

	public JsonObjectBuilder createYamlPlanProperties(boolean versioned) {
		// plan only has "name", "version", and the root artifact
		JsonObjectBuilder objectBuilder = jsonProvider.createObjectBuilder();
		objectBuilder.add("name", jsonProvider.createObjectBuilder().add("type", "string"));

		if (versioned) {
			// in 'version' we should either explicitly specify the current json schema version or skip this field
			objectBuilder.add("version", jsonProvider.createObjectBuilder().add("const", actualVersion.toString()));
		}
		objectBuilder.add("root", YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), ROOT_ARTEFACT_DEF));
		return objectBuilder;
	}

	/**
	 * Prepares definitions to be reused in json subschemas
	 */
	public JsonObjectBuilder createDefs() throws JsonSchemaPreparationException {
		JsonObjectBuilder defsBuilder = jsonProvider.createObjectBuilder();

		List<JsonSchemaDefinitionCreator> definitionCreators = new ArrayList<>();

		// prepare definitions for generic DynamicValue class
		definitionCreators.add((defsList) -> {
			Map<String, JsonObjectBuilder> dynamicValueDefs = schemaHelper.createDynamicValueImplDefs();
			for (Map.Entry<String, JsonObjectBuilder> dynamicValueDef : dynamicValueDefs.entrySet()) {
				defsBuilder.add(dynamicValueDef.getKey(), dynamicValueDef.getValue());
			}
		});

		// prepare definitions for referenced resources
		definitionCreators.add(defsList -> {
			Map<String, JsonObjectBuilder> dynamicValueDefs = resourceReferenceJsonSchemaHelper.createResourceReferenceDefs();
			for (Map.Entry<String, JsonObjectBuilder> dynamicValueDef : dynamicValueDefs.entrySet()) {
				defsBuilder.add(dynamicValueDef.getKey(), dynamicValueDef.getValue());
			}
		});

		// prepare definitions for referenced resources
		definitionCreators.add(defsList -> {
			Map<String, JsonObjectBuilder> dynamicValueDefs = resourceReferenceJsonSchemaHelper.createResourceReferenceDefs();
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
			defsBuilder.add(ARTEFACT_DEF, createArtefactDef(artefactImplDefs.controlArtefactDefs));
			defsBuilder.add(ROOT_ARTEFACT_DEF, createArtefactDef(artefactImplDefs.rootArtefactDefs));
			defsBuilder.add(AbstractYamlArtefact.ARTEFACT_ARRAY_DEF, createArtefactArrayDef());
		});

		// add definitions from extensions (additional definitions for EE artefacts)
		definitionCreators.addAll(getDefinitionsExtensions());

		for (JsonSchemaDefinitionCreator definitionCreator : definitionCreators) {
			definitionCreator.addDefinition(defsBuilder);
		}

		return defsBuilder;
	}

	private JsonObjectBuilder createArtefactArrayDef() {
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		builder.add("type", "array");
		builder.add("items", YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), ARTEFACT_DEF));
		return builder;
	}

	private ArtefactDefinitions createArtefactImplDefs() throws JsonSchemaPreparationException {
		ArtefactDefinitions artefactDefinitions = new ArtefactDefinitions();

		// scan all @Artefact classes in classpath and automatically prepare definitions for them
		List<Class<? extends AbstractYamlArtefact<?>>> artefactClasses = YamlArtefactsLookuper.getYamlArtefactClasses();
		log.info("The following {} artefact classes detected: {}", artefactClasses.size(), artefactClasses);

		// find allowed control artefacts
		Set<Class<?>> controlArtefactClasses = artefactClasses.stream().filter(YamlArtefactsLookuper::isControlArtefact).collect(Collectors.toSet());

		// find allowed root artefacts
		Set<Class<?>> rootArtefactClasses = artefactClasses.stream().filter(YamlArtefactsLookuper::isRootArtefact).collect(Collectors.toSet());
		log.info("The following {} artefact classes detected: {}", rootArtefactClasses.size(), rootArtefactClasses);

		for (Class<? extends AbstractYamlArtefact<?>> yamlArtefactClass : artefactClasses) {
			// use the name of artefact as definition name
			String name = YamlArtefactsLookuper.getYamlArtefactName(yamlArtefactClass);
			String defName = name + "Def";

			// scan all fields in artefact class and put them to artefact definition
			JsonObjectBuilder def = createArtefactImplDef(name, yamlArtefactClass);
			artefactDefinitions.allArtefactDefs.put(defName, def);

			// for root artefacts we only support the subset of all artefact definitions
			if (rootArtefactClasses.contains(yamlArtefactClass)) {
				artefactDefinitions.rootArtefactDefs.add(defName);
			}
			if (controlArtefactClasses.contains(yamlArtefactClass)) {
				artefactDefinitions.controlArtefactDefs.add(defName);
			}
		}
		return artefactDefinitions;
	}

	private JsonObjectBuilder createArtefactImplDef(String name, Class<? extends AbstractYamlArtefact<?>> yamlArtefactClass) throws JsonSchemaPreparationException {
		// artefact has the top-level property matching the artefact name
		return schemaHelper.createNamedObjectImplDef(
				YamlArtefactsLookuper.getYamlArtefactName(yamlArtefactClass),
				yamlArtefactClass,
				jsonSchemaCreator,
				true
		);
	}

	private JsonObjectBuilder createArtefactDef(Collection<String> artefactImplReferences) {
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		builder.add("type", "object");
		JsonArrayBuilder arrayBuilder = jsonProvider.createArrayBuilder();
		for (String artefactImplReference : artefactImplReferences) {
			arrayBuilder.add(YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), artefactImplReference));
		}
		builder.add("oneOf", arrayBuilder);
		return builder;
	}

	private JsonNode fromJakartaToJsonNode(JsonObjectBuilder objectBuilder) throws JsonProcessingException {
		return objectMapper.readTree(objectBuilder.build().toString());
	}

	private static class ArtefactDefinitions {
		private final Map<String, JsonObjectBuilder> allArtefactDefs = new HashMap<>();

		private final List<String> controlArtefactDefs = new ArrayList<>();
		private final List<String> rootArtefactDefs = new ArrayList<>();
	}

}
