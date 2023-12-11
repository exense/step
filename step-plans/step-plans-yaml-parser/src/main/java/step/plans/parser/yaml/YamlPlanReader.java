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
package step.plans.parser.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Version;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.core.scanner.CachedAnnotationScanner;
import step.migration.MigrationManager;
import step.plans.nl.RootArtefactType;
import step.plans.nl.parser.PlanParser;
import step.core.yaml.deserializers.YamlDynamicValueDeserializer;
import step.plans.parser.yaml.deserializers.UpgradableYamlPlanDeserializer;
import step.plans.parser.yaml.deserializers.YamlRootArtefactDeserializer;
import step.plans.parser.yaml.migrations.AbstractYamlPlanMigrationTask;
import step.plans.parser.yaml.migrations.YamlPlanMigration;
import step.plans.parser.yaml.model.YamlPlan;
import step.plans.parser.yaml.model.YamlPlanVersions;
import step.plans.parser.yaml.model.YamlRootArtefact;
import step.plans.parser.yaml.rules.NodeNameRule;
import step.plans.parser.yaml.schema.YamlPlanValidationException;
import step.core.yaml.serializers.YamlDynamicValueSerializer;
import step.plans.parser.yaml.serializers.YamlRootArtefactSerializer;
import step.repositories.parser.StepsParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static step.core.scanner.Classes.newInstanceAs;

public class YamlPlanReader {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanReader.class);

	public static final String YAML_PLANS_COLLECTION_NAME = "yamlPlans";

	private final ObjectMapper yamlMapper;

	private final ObjectMapper simpleJsonObjectMapper = DefaultJacksonMapperProvider.getObjectMapper();

	private final Supplier<ObjectId> idGenerator;
	private final Version currentVersion;
	private String jsonSchema;
	private final MigrationManager migrationManager;
	private final PlanParser plainTextPlanParser;

	/**
	 * To be used in TESTS only
	 *
	 * @param idGenerator            the id generator to generate static ids in tests
	 * @param currentVersion         the fixed yaml format version. If null, the actual version will be used
	 * @param validateWithJsonSchema if true, the json schema will be used to validate yaml plans
	 * @param jsonSchemaPath         the fixed path to json schema. If null, the actual json schema will be used
	 */
	public YamlPlanReader(Supplier<ObjectId> idGenerator, Version currentVersion, boolean validateWithJsonSchema, String jsonSchemaPath) {
		if (currentVersion != null) {
			this.currentVersion = currentVersion;
		} else {
			this.currentVersion = YamlPlanVersions.ACTUAL_VERSION;
		}

		// TODO: several same messages in log on server startup => remove this log?
		log.info("YAML Plans version: {}", this.currentVersion);

		if (validateWithJsonSchema) {
			if (jsonSchemaPath != null) {
				this.jsonSchema = readJsonSchema(jsonSchemaPath);
			} else {
				// resolve the json schema to use
				List<String> jsonSchemasFromExtensions = CachedAnnotationScanner.getClassesWithAnnotation(YamlPlanMigration.LOCATION, YamlPlanReaderExtension.class, Thread.currentThread().getContextClassLoader())
						.stream()
						.map(newInstanceAs(YamlPlanReaderExtender.class))
						.map(YamlPlanReaderExtender::getJsonSchemaPath)
						.filter(Objects::nonNull)
						.collect(Collectors.toList());

				String resolvedJsonSchemaPath;

				if (!jsonSchemasFromExtensions.isEmpty()) {
					if (jsonSchemasFromExtensions.size() > 1) {
						throw new IllegalArgumentException("Ambiguous json schema is defined in project: " + jsonSchemasFromExtensions);
					}
					resolvedJsonSchemaPath = jsonSchemasFromExtensions.get(0);
				} else {
					resolvedJsonSchemaPath = YamlPlanVersions.ACTUAL_JSON_SCHEMA_PATH;
				}

				log.info("The json schema for yaml plans: {}", resolvedJsonSchemaPath);
				this.jsonSchema = readJsonSchema(resolvedJsonSchemaPath);
			}
		}

		this.idGenerator = idGenerator;
		this.migrationManager = initMigrationManager();
		this.plainTextPlanParser = new PlanParser();

		this.yamlMapper = createYamlPlanObjectMapper();
	}

	/**
	 * Creates the new Yaml plan serializer for the specified current version and json schema
	 */
	public YamlPlanReader(Version currentVersion, String jsonSchemaPath) {
		this(null, currentVersion, true, jsonSchemaPath);
	}

	/**
	 * Creates the new Yaml plan reader with actual version and json schema
	 */
	public YamlPlanReader(){
		this(null, null, true, null);
	}

	/**
	 * Read the plan from Yaml
	 *
	 * @param yamlPlanStream yaml data
	 */
	public Plan readYamlPlan(InputStream yamlPlanStream) throws IOException, YamlPlanValidationException {
		YamlPlan yamlPlan = yamlMapper.readValue(yamlPlanStream, YamlPlan.class);
		return yamlPlanToPlan(yamlPlan);
	}

	/**
	 * Writes the plan as YAML
	 */
	public void writeYamlPlan(OutputStream os, Plan plan) throws IOException {
		yamlMapper.writeValue(os, planToYamlPlan(plan));
	}

	public void convertFromPlainTextToYaml(String planName, InputStream planTextInputStream, OutputStream yamlOutputStream) throws IOException, StepsParser.ParsingException {
		// read plan from plain text format
		Plan planFromPlainText = plainTextPlanParser.parse(planTextInputStream, RootArtefactType.TestCase);

		// set file name as plan name
		Map<String, String> attributes = new HashMap<>();
		attributes.put(AbstractArtefact.NAME, planName);
		planFromPlainText.setAttributes(attributes);
		planFromPlainText.getRoot().getAttributes().put(AbstractArtefact.NAME, planName);

		// apply default values for plain text artefact
		applyDefaultValuesForPlainTextArtefact(planFromPlainText.getRoot());

		// convert to simple yaml and save in output file
		writeYamlPlan(yamlOutputStream, planFromPlainText);
	}

	private void applyDefaultValuesForPlainTextArtefact(AbstractArtefact plainTextArtefact) throws JsonProcessingException {
		String artefactClassName = AbstractArtefact.getArtefactName(plainTextArtefact.getClass());

		// if artefact name is default (filled by plainTextPlanParser), we replace the value with default in terms of yaml format
		// (for example, use a keyword name as artefact name)
		if (Objects.equals(plainTextArtefact.getAttribute(AbstractArtefact.NAME), artefactClassName)) {
			plainTextArtefact.addAttribute(AbstractOrganizableObject.NAME, NodeNameRule.defaultNodeName(plainTextArtefact, simpleJsonObjectMapper));
		}

		for (AbstractArtefact child : plainTextArtefact.getChildren()) {
			applyDefaultValuesForPlainTextArtefact(child);
		}
	}

	protected ObjectMapper createYamlPlanObjectMapper() {
		ObjectMapper yamlMapper = createDefaultYamlMapper();

		// configure custom deserializers
		yamlMapper.registerModule(registerAllSerializers(new SimpleModule(), yamlMapper));
		return yamlMapper;
	}

	private static ObjectMapper createDefaultYamlMapper() {
		YAMLFactory yamlFactory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		return DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);
	}

	private SimpleModule createDatabindModuleForNonUpgradablePlans(ObjectMapper resultingMapper) {
		SimpleModule module = new SimpleModule();
		registerBasicSerializers(module, resultingMapper);
		return module;
	}

	private SimpleModule registerBasicSerializers(SimpleModule module, ObjectMapper resultingMapper) {
		module.addDeserializer(DynamicValue.class, new YamlDynamicValueDeserializer());
		module.addDeserializer(YamlRootArtefact.class, createRootArtefactDeserializer(resultingMapper));

		module.addSerializer(DynamicValue.class, new YamlDynamicValueSerializer());
		return module.addSerializer(YamlRootArtefact.class, createRootArtefactSerializer(resultingMapper));
	}

	public SimpleModule registerAllSerializers(SimpleModule module, ObjectMapper resultingMapper) {
		ObjectMapper nonUpgradableYamlMapper = createDefaultYamlMapper().registerModule(createDatabindModuleForNonUpgradablePlans(resultingMapper));

		return registerBasicSerializers(module, resultingMapper)
				.addDeserializer(YamlPlan.class, new UpgradableYamlPlanDeserializer(currentVersion, jsonSchema, migrationManager, nonUpgradableYamlMapper));
	}

	protected YamlRootArtefactSerializer createRootArtefactSerializer(ObjectMapper stepYamlMapper) {
		return new YamlRootArtefactSerializer(stepYamlMapper);
	}

	protected YamlRootArtefactDeserializer createRootArtefactDeserializer(ObjectMapper stepYamlMapper) {
		return new YamlRootArtefactDeserializer(stepYamlMapper);
	}

	protected ObjectMapper getYamlMapper() {
		return yamlMapper;
	}

	protected String readJsonSchema(String jsonSchemaPath) {
		try (InputStream jsonSchemaInputStream = this.getClass().getClassLoader().getResourceAsStream(jsonSchemaPath)) {
			if (jsonSchemaInputStream == null) {
				throw new IllegalStateException("Json schema not found: " + jsonSchemaPath);
			}
			return new String(jsonSchemaInputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load json schema: " + jsonSchemaPath, e);
		}
	}

	/**
	 * Initializes the migration manager with specific migrations used for yaml plan format
	 */
	protected MigrationManager initMigrationManager() {
		final MigrationManager migrationManager = new MigrationManager();

		try (AnnotationScanner annotationScanner = AnnotationScanner.forAllClassesFromClassLoader(YamlPlanMigration.LOCATION, Thread.currentThread().getContextClassLoader())) {
			Set<Class<?>> migrations = annotationScanner.getClassesWithAnnotation(YamlPlanMigration.class);
			for (Class<?> migration : migrations) {
				if (!AbstractYamlPlanMigrationTask.class.isAssignableFrom(migration)) {
					throw new IllegalArgumentException("Class " + migration + " doesn't extend the " + AbstractYamlPlanMigrationTask.class);
				}
				migrationManager.register((Class<? extends AbstractYamlPlanMigrationTask>) migration);
			}
		}

		return migrationManager;
	}

	public Plan yamlPlanToPlan(YamlPlan yamlPlan) {
		Plan plan = new Plan(yamlPlan.getRoot().getAbstractArtefact());
		plan.addAttribute(AbstractOrganizableObject.NAME, yamlPlan.getName());
		applyDefaultValues(plan);
		return plan;
	}

	protected YamlPlan planToYamlPlan(Plan plan){
		YamlPlan yamlPlan = new YamlPlan();
		yamlPlan.setName(plan.getAttribute(AbstractOrganizableObject.NAME));
		yamlPlan.setVersion(currentVersion.toString());
		yamlPlan.setRoot(new YamlRootArtefact(plan.getRoot()));
		return yamlPlan;
	}

	private void applyDefaultValues(Plan plan) {
		if (this.idGenerator != null) {
			plan.setId(this.idGenerator.get());
		}

		AbstractArtefact root = plan.getRoot();
		if (root != null) {
			applyDefaultValuesForArtifact(root);
		}
	}

	private void applyDefaultValuesForArtifact(AbstractArtefact artifact) {
		if (this.idGenerator != null) {
			artifact.setId(this.idGenerator.get());
		}
		applyDefaultValuesForChildren(artifact);
	}

	private void applyDefaultValuesForChildren(AbstractArtefact root) {
		List<AbstractArtefact> children = root.getChildren();
		if (children != null) {
			for (AbstractArtefact child : children) {
				applyDefaultValuesForArtifact(child);
			}
		}
	}

}
