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
import step.core.plans.agents.configuration.AgentPoolProvisioningConfiguration;
import step.core.Version;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.agents.configuration.AutomaticAgentProvisioningConfiguration;
import step.core.scanner.CachedAnnotationScanner;
import step.core.yaml.schema.*;
import step.handlers.javahandler.jsonschema.FieldMetadataExtractor;
import step.handlers.javahandler.jsonschema.JsonSchemaCreator;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.jsonschema.DefaultFieldMetadataExtractor;
import step.core.yaml.YamlArtefactsLookuper;
import step.plans.parser.yaml.YamlPlanFields;
import step.core.yaml.model.AbstractYamlArtefact;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static step.core.plans.agents.configuration.ManualAgentProvisioningConfiguration.AGENT_POOL_CONFIGURATION_ARRAY_DEF;
import static step.core.plans.agents.configuration.ManualAgentProvisioningConfiguration.AGENT_CONFIGURATION_YAML_NAME;
import static step.core.scanner.Classes.newInstanceAs;

public class YamlPlanJsonSchemaGenerator {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanJsonSchemaGenerator.class);

	public static final String ARTEFACT_DEF = "ArtefactDef";
	private static final String ROOT_ARTEFACT_DEF = "RootArtefactDef";
	public static final String SchemaDefSuffix = "Def";

	protected final String targetPackage;

	protected final Version actualVersion;
	private final String schemaId;

	protected final ObjectMapper objectMapper = DefaultJacksonMapperProvider.getObjectMapper();
	protected final JsonProvider jsonProvider = JsonProvider.provider();

	protected final JsonSchemaCreator jsonSchemaCreator;
	protected final YamlJsonSchemaHelper schemaHelper = new YamlJsonSchemaHelper(jsonProvider);

	public YamlPlanJsonSchemaGenerator(String targetPackage, Version actualVersion, String schemaId) {
		this.targetPackage = targetPackage;
		this.actualVersion = actualVersion;
		this.schemaId = schemaId;

		// --- Fields metadata rules (fields we want to rename)
		FieldMetadataExtractor fieldMetadataExtractor = prepareMetadataExtractor();

		List<JsonSchemaFieldProcessor> processingRules = YamlJsonSchemaHelper.prepareDefaultFieldProcessors(null);
		this.jsonSchemaCreator = new JsonSchemaCreator(jsonProvider, new AggregatedJsonSchemaFieldProcessor(processingRules), fieldMetadataExtractor);
	}

	protected List<JsonSchemaExtension> getDefinitionsExtensions() {
		List<JsonSchemaExtension> extensions = new ArrayList<>();
		CachedAnnotationScanner.getClassesWithAnnotation(JsonSchemaDefinitionAddOn.LOCATION, JsonSchemaDefinitionAddOn.class, Thread.currentThread().getContextClassLoader()).stream()
				.map(newInstanceAs(JsonSchemaExtension.class)).forEach(extensions::add);
		return extensions;
	}

	public static FieldMetadataExtractor prepareMetadataExtractor() {
		List<FieldMetadataExtractor> metadataExtractors = new ArrayList<>();
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

		// prepare definitions to be reused in subschemas (referenced via $ref property)
		topLevelBuilder.add("$defs", createDefs(true));

		YamlJsonSchemaHelper.addRef(topLevelBuilder, YamlJsonSchemaHelper.PLAN_DEF);

		// convert jakarta objects to jackson JsonNode
		try {
			return fromJakartaToJsonNode(topLevelBuilder);
		} catch (JsonProcessingException e) {
			throw new JsonSchemaPreparationException("Unable to convert json to jackson jsonNode", e);
		}
	}

	public JsonObjectBuilder createYamlPlanProperties(boolean versioned, boolean isCompositePlan) {
		// plan only has "name", "version", and the root artifact
		JsonObjectBuilder objectBuilder = jsonProvider.createObjectBuilder();
		objectBuilder.add("name", jsonProvider.createObjectBuilder().add("type", "string"));

		if (versioned) {
			// in 'version' we should either explicitly specify the current json schema version or skip this field
			objectBuilder.add("version", jsonProvider.createObjectBuilder().add("const", actualVersion.toString()));
		}
		if (!isCompositePlan) {
			//categories
			JsonObjectBuilder categoriesBuilder = jsonProvider.createObjectBuilder();
			categoriesBuilder.add("type", "array");
			categoriesBuilder.add("items", jsonProvider.createObjectBuilder().add("type","string"));
			objectBuilder.add("categories", categoriesBuilder);
			//agents
			objectBuilder.add(AGENT_CONFIGURATION_YAML_NAME, YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), AGENT_CONFIGURATION_YAML_NAME + SchemaDefSuffix));
		}
		objectBuilder.add("root", YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), ROOT_ARTEFACT_DEF));
		return objectBuilder;
	}

	/**
	 * Prepares definitions to be reused in json subschemas
	 */
	public JsonObjectBuilder createDefs(boolean versionedPlans) throws JsonSchemaPreparationException {
		JsonObjectBuilder defsBuilder = jsonProvider.createObjectBuilder();

        // add definitions from extensions
        List<JsonSchemaExtension> definitionCreators = new ArrayList<>(getDefinitionsExtensions());

		// prepare definitions for artefacts
		definitionCreators.add((defsList, schemaCreator) -> {
			ArtefactDefinitions artefactImplDefs = createArtefactImplDefs();

			// sort definition to keep the stable ordering in json schema
			List<Map.Entry<String, JsonObjectBuilder>> arefactEntries = new ArrayList<>(artefactImplDefs.allArtefactDefs.entrySet());
			arefactEntries.sort(Map.Entry.comparingByKey());

			for (Map.Entry<String, JsonObjectBuilder> artefactImplDef : arefactEntries) {
				defsBuilder.add(artefactImplDef.getKey(), artefactImplDef.getValue());
			}

			defsBuilder.add(ARTEFACT_DEF, createArtefactDef(artefactImplDefs.controlArtefactDefs));
			defsBuilder.add(ROOT_ARTEFACT_DEF, createArtefactDef(artefactImplDefs.rootArtefactDefs));
			defsBuilder.add(AbstractYamlArtefact.ARTEFACT_ARRAY_DEF, createArtefactArrayDef());
			createPlanPoolConfigurationsDef(defsBuilder);

			defsBuilder.add(YamlJsonSchemaHelper.PLAN_DEF, createPlanDef(versionedPlans, false));
			defsBuilder.add(YamlJsonSchemaHelper.COMPOSITE_PLAN_DEF, createPlanDef(versionedPlans, true));
		});

		for (JsonSchemaExtension definitionCreator : definitionCreators) {
			definitionCreator.addToJsonSchema(defsBuilder, jsonProvider);
		}

		return defsBuilder;
	}

	private JsonObjectBuilder createArtefactArrayDef() {
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		builder.add("type", "array");
		builder.add("items", YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), ARTEFACT_DEF));
		return builder;
	}

	private void createPlanPoolConfigurationsDef(JsonObjectBuilder defsBuilder) throws JsonSchemaPreparationException {
		//Create definition for agentPoolConfiguration
		String agentPoolConfigurationYamlName = "agentPoolConfiguration";
		defsBuilder.add(agentPoolConfigurationYamlName + SchemaDefSuffix, schemaHelper.createJsonSchemaForClass(
				jsonSchemaCreator,
				AgentPoolProvisioningConfiguration.class,
				true
		));

		//Create definition for array of agentPoolConfigurationYamlName
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		builder.add("type", "array");
		builder.add("items", YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), agentPoolConfigurationYamlName + SchemaDefSuffix));
		defsBuilder.add(AGENT_POOL_CONFIGURATION_ARRAY_DEF, builder);

		//Create definition for agents configuration in plan (One of)
		JsonArrayBuilder arrayBuilder = jsonSchemaCreator.getJsonProvider().createArrayBuilder();
		//add list of agent spec configurations
		arrayBuilder.add(YamlJsonSchemaHelper.addRef(jsonProvider.createObjectBuilder(), AGENT_POOL_CONFIGURATION_ARRAY_DEF));
		//Add enum
		JsonObjectBuilder enumBuilder = jsonSchemaCreator.getJsonProvider().createObjectBuilder();
		JsonArrayBuilder enumArray = jsonSchemaCreator.getJsonProvider().createArrayBuilder();
		for (Object enumValue : AutomaticAgentProvisioningConfiguration.PlanAgentsPoolAutoMode.values()) {
			enumArray.add(enumValue.toString());
		}
		enumBuilder.add("enum", enumArray);
		arrayBuilder.add(enumBuilder);

		//Create agentsDef with oneOf
		JsonObjectBuilder agentsDefBuilder = jsonProvider.createObjectBuilder();
		agentsDefBuilder.add("oneOf", arrayBuilder);
		defsBuilder.add(AGENT_CONFIGURATION_YAML_NAME + SchemaDefSuffix, agentsDefBuilder);
	}

	private ArtefactDefinitions createArtefactImplDefs() throws JsonSchemaPreparationException {
		ArtefactDefinitions artefactDefinitions = new ArtefactDefinitions();

		List<Class<? extends AbstractYamlArtefact<?>>> specialYamlModels = YamlArtefactsLookuper.getSpecialYamlArtefactModels();
		log.info("The following {} special artefact yaml models detected: {}", specialYamlModels.size(), specialYamlModels);

		List<Class<? extends AbstractArtefact>> simpleYamlModels = YamlArtefactsLookuper.getSimpleYamlArtefactModels();
		log.info("The following {} simple artefact yaml models detected: {}", simpleYamlModels.size(), simpleYamlModels);

		// find allowed control artefacts
		Set<Class<?>> controlArtefactClasses = Stream.concat(specialYamlModels.stream(), simpleYamlModels.stream())
				.filter(YamlArtefactsLookuper::isControlArtefact)
				.collect(Collectors.toSet());

		// find allowed root artefacts
		Set<Class<?>> rootArtefactClasses = Stream.concat(specialYamlModels.stream(), simpleYamlModels.stream())
				.filter(YamlArtefactsLookuper::isRootArtefact)
				.collect(Collectors.toSet());
		log.info("The following {} root artefact classes detected: {}", rootArtefactClasses.size(), rootArtefactClasses);

		// prepare json schemas for SPECIAL models
		for (Class<? extends AbstractYamlArtefact<?>> yamlArtefactClass : specialYamlModels) {
			// use the name of artefact as definition name
			Class<? extends AbstractArtefact> artefactClass = YamlArtefactsLookuper.getArtefactClass(yamlArtefactClass);

			if (artefactClass != null) {
				String name = AbstractArtefact.getArtefactName(artefactClass);
				String defName = name + SchemaDefSuffix;

				JsonObjectBuilder def = schemaHelper.createNamedObjectImplDef(
						YamlArtefactsLookuper.getYamlArtefactName(artefactClass),
						yamlArtefactClass,
						jsonSchemaCreator,
						true
				);

				artefactDefinitions.allArtefactDefs.put(defName, def);

				// for root artefacts we only support the subset of all artefact definitions
				if (rootArtefactClasses.contains(yamlArtefactClass)) {
					artefactDefinitions.rootArtefactDefs.add(defName);
				}
				if (controlArtefactClasses.contains(yamlArtefactClass)) {
					artefactDefinitions.controlArtefactDefs.add(defName);
				}
			}
		}

		// prepare json schemas for SIMPLE models
		for (Class<? extends AbstractArtefact> simpleYamlModel : simpleYamlModels) {
			// use the name of artefact as definition name
			Class<? extends AbstractArtefact> artefactClass = YamlArtefactsLookuper.getArtefactClass(simpleYamlModel);

			if (artefactClass != null) {
				String name = AbstractArtefact.getArtefactName(artefactClass);
				String defName = name + SchemaDefSuffix;

				JsonObjectBuilder def = createJsonSchemaForSimpleArtefact(simpleYamlModel);

				artefactDefinitions.allArtefactDefs.put(defName, def);

				// for root artefacts we only support the subset of all artefact definitions
				if (rootArtefactClasses.contains(simpleYamlModel)) {
					artefactDefinitions.rootArtefactDefs.add(defName);
				}
				if (controlArtefactClasses.contains(simpleYamlModel)) {
					artefactDefinitions.controlArtefactDefs.add(defName);
				}
			}
		}

		// fixed artefact ordering in json schema
		artefactDefinitions.rootArtefactDefs.sort(Comparator.naturalOrder());
		artefactDefinitions.controlArtefactDefs.sort(Comparator.naturalOrder());

		return artefactDefinitions;
	}

	private JsonObjectBuilder createJsonSchemaForSimpleArtefact(Class<? extends AbstractArtefact> simpleYamlArtefact) throws JsonSchemaPreparationException {
		JsonObjectBuilder res = jsonProvider.createObjectBuilder();
		res.add("type", "object");

		// on the top level there is a name only
		JsonObjectBuilder schemaBuilder = jsonProvider.createObjectBuilder();

		// other properties are located in nested object and automatically prepared via reflection
		JsonObjectBuilder propertiesBuilder = jsonProvider.createObjectBuilder();
		propertiesBuilder.add("type", "object");

		JsonObjectBuilder classPropertiesBuilder = jsonProvider.createObjectBuilder();

		List<String> requiredProperties = new ArrayList<>();

		// properties declared in AbstractArtefact are taken from AbstractYamlArtefact
		schemaHelper.extractPropertiesFromClass(jsonSchemaCreator, AbstractYamlArtefact.class, classPropertiesBuilder, requiredProperties, null);
		schemaHelper.extractPropertiesFromClass(jsonSchemaCreator, simpleYamlArtefact, classPropertiesBuilder, requiredProperties, AbstractArtefact.class);
		
		// define the default node name explicitly
		// TODO: find some solution to apply default 'nodeName'
		classPropertiesBuilder.add(YamlPlanFields.NAME_YAML_FIELD,
				jsonProvider.createObjectBuilder()
						.add("$ref", "#/$defs/SmartDynamicValueStringDef")
						.add("default", new AbstractYamlArtefact.DefaultYamlArtefactNameProvider().getDefaultValue(simpleYamlArtefact, null))
		);

		propertiesBuilder.add("properties", classPropertiesBuilder);
		schemaHelper.addRequiredProperties(requiredProperties, propertiesBuilder);

		schemaBuilder.add(YamlArtefactsLookuper.getYamlArtefactName(simpleYamlArtefact), propertiesBuilder);
		res.add("properties", schemaBuilder);
		res.add("additionalProperties", false);
		return res;
	}

	private JsonObjectBuilder createPlanDef(boolean versionedPlans, boolean isCompositePlan) {
		JsonObjectBuilder yamlPlanDef = jsonProvider.createObjectBuilder();
		// add properties for top-level "plan" object
		yamlPlanDef.add("properties", createYamlPlanProperties(versionedPlans, isCompositePlan));
		JsonArrayBuilder arrayBuilder = jsonProvider.createArrayBuilder();
		if (!isCompositePlan) {
			// for composite plan the name is not required
			arrayBuilder.add("name");
		}
		arrayBuilder.add("root");
		yamlPlanDef.add("required", arrayBuilder);
		yamlPlanDef.add("additionalProperties", false);
		return yamlPlanDef;
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
