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

import com.fasterxml.jackson.annotation.JsonInclude;
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
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.core.scanner.CachedAnnotationScanner;
import step.core.yaml.deserializers.StepYamlDeserializersScanner;
import step.core.yaml.serializers.StepYamlSerializersScanner;
import step.migration.MigrationManager;
import step.plans.nl.RootArtefactType;
import step.plans.nl.parser.PlanParser;
import step.plans.parser.yaml.deserializers.UpgradableYamlPlanDeserializer;
import step.plans.parser.yaml.migrations.AbstractYamlPlanMigrationTask;
import step.plans.parser.yaml.migrations.YamlPlanMigration;
import step.plans.parser.yaml.model.AbstractYamlArtefact;
import step.plans.parser.yaml.model.NamedYamlArtefact;
import step.plans.parser.yaml.model.YamlPlan;
import step.plans.parser.yaml.model.YamlPlanVersions;
import step.plans.parser.yaml.schema.YamlPlanValidationException;
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

	public static void setPlanName(Plan plan, String name) {
		Map<String, String> attributes = new HashMap<>();
		attributes.put(AbstractArtefact.NAME, name);
		plan.setAttributes(attributes);
		plan.getRoot().getAttributes().put(AbstractArtefact.NAME, name);
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

		// apply cleanup default values from plain text artefact to omit them in yaml format
		cleanupDefaultNodeNames(planFromPlainText.getRoot());

		// convert to simple yaml and save in output file
		writeYamlPlan(yamlOutputStream, planFromPlainText);
	}

	private void cleanupDefaultNodeNames(AbstractArtefact plainTextArtefact) throws JsonProcessingException {
		String artefactClassName = AbstractArtefact.getArtefactName(plainTextArtefact.getClass());

		// if artefact name is default (filled by plainTextPlanParser), we replace the value with default in terms of yaml format
		// (for example, use a keyword name as artefact name)
		if (Objects.equals(plainTextArtefact.getAttribute(AbstractArtefact.NAME), artefactClassName)) {
			plainTextArtefact.addAttribute(AbstractOrganizableObject.NAME, null);
		}

		for (AbstractArtefact child : plainTextArtefact.getChildren()) {
			cleanupDefaultNodeNames(child);
		}
	}

	protected ObjectMapper createYamlPlanObjectMapper() {
		ObjectMapper yamlMapper = createDefaultYamlMapper();
		// default: include only non empty values in yaml
		yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

		// configure custom deserializers
		yamlMapper.registerModule(registerAllSerializersAndDeserializers(new SimpleModule(), yamlMapper, true));
		return yamlMapper;
	}

	private static ObjectMapper createDefaultYamlMapper() {
		YAMLFactory yamlFactory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		return DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);
	}

	private SimpleModule registerBasicSerializersAndDeserializers(SimpleModule module, ObjectMapper resultingMapper) {
		SimpleModule res = StepYamlDeserializersScanner.addAllDeserializerAddonsToModule(module, resultingMapper);
		res = StepYamlSerializersScanner.addAllSerializerAddonsToModule(res, resultingMapper);
		return res;
	}

	public SimpleModule registerAllSerializersAndDeserializers(SimpleModule module, ObjectMapper resultingMapper, boolean upgradablePlan) {
		ObjectMapper nonUpgradableYamlMapper = createDefaultYamlMapper().registerModule(createModuleForNonUpgradablePlans(resultingMapper));

		return registerBasicSerializersAndDeserializers(module, resultingMapper)
				.addDeserializer(YamlPlan.class, new UpgradableYamlPlanDeserializer(upgradablePlan ? currentVersion : null, jsonSchema, migrationManager, nonUpgradableYamlMapper));
	}

	private SimpleModule createModuleForNonUpgradablePlans(ObjectMapper resultingMapper) {
		SimpleModule module = new SimpleModule();
		registerBasicSerializersAndDeserializers(module, resultingMapper);
		return module;
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
		Plan plan = new Plan(yamlPlan.getRoot().getYamlArtefact().toArtefact());
		setPlanName(plan, yamlPlan.getName());
		applyDefaultValues(plan);
		return plan;
	}

	protected YamlPlan planToYamlPlan(Plan plan){
		YamlPlan yamlPlan = new YamlPlan();
		yamlPlan.setName(plan.getAttribute(AbstractOrganizableObject.NAME));
		yamlPlan.setVersion(currentVersion.toString());
		yamlPlan.setRoot(new NamedYamlArtefact(AbstractYamlArtefact.toYamlArtefact(plan.getRoot(), yamlMapper)));
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
