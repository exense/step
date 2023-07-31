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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.bson.types.ObjectId;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.handlers.JsonSchemaValidator;
import step.core.Version;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.artefacts.AbstractArtefact;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.scanner.CachedAnnotationScanner;
import step.migration.MigrationManager;
import step.plans.parser.yaml.model.YamlPlan;
import step.plans.parser.yaml.deserializers.YamlDynamicValueDeserializer;
import step.plans.parser.yaml.deserializers.YamlRootArtefactDeserializer;
import step.plans.parser.yaml.migrations.AbstractYamlPlanMigrationTask;
import step.plans.parser.yaml.model.YamlRootArtefact;
import step.plans.parser.yaml.model.YamlPlanVersions;
import step.plans.parser.yaml.serializers.YamlDynamicValueSerializer;
import step.plans.parser.yaml.serializers.YamlRootArtefactSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

import static step.core.scanner.Classes.newInstanceAs;

public class YamlPlanReader {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanReader.class);

	public static final String YAML_PLANS_COLLECTION_NAME = "yamlPlans";

	private final ObjectMapper yamlMapper;
	private final ObjectMapper technicalPlanMapper;

	private final Supplier<ObjectId> idGenerator;
	private Version currentVersion;
	private String jsonSchema;
	private final MigrationManager migrationManager;

	// for tests
	YamlPlanReader(Supplier<ObjectId> idGenerator, YamlPlanVersions.YamlPlanVersion currentVersion) {
		if (currentVersion != null) {
			setCurrentVersion(currentVersion);
		} else {
			// resolve version and schema automatically
			YamlPlanSerializerExtender.ExtendedYamlPlanVersion versionFromExtensions = CachedAnnotationScanner.getClassesWithAnnotation(YamlPlanSerializerExtension.class).stream()
					.map(newInstanceAs(YamlPlanSerializerExtender.class))
					.map(YamlPlanSerializerExtender::getYamlPlanVersion)
					.filter(Objects::nonNull).min(Comparator.comparingInt(YamlPlanSerializerExtender.ExtendedYamlPlanVersion::getPriority))
					.orElse(null);

			if(versionFromExtensions != null){
				setCurrentVersion(versionFromExtensions.getVersion());
			} else {
				setCurrentVersion(YamlPlanVersions.ACTUAL_VERSION);
			}
		}
		this.yamlMapper = createYamlPlanObjectMapper();
		this.technicalPlanMapper = createTechnicalPlanObjectMapper();
		this.idGenerator = idGenerator;
		this.migrationManager = initMigrationManager();
	}

	/**
	 * Creates the new Yaml plan serializer for the specified current version (json schema)
	 * @param currentVersion the current Yaml format version (and json schema)
	 */
	public YamlPlanReader(YamlPlanVersions.YamlPlanVersion currentVersion) {
		this(null, currentVersion);
	}

	private void setCurrentVersion(YamlPlanVersions.YamlPlanVersion currentVersion) {
		this.currentVersion = currentVersion.getVersion();
		if (currentVersion.getJsonSchemaPath() != null) {
			this.jsonSchema = readJsonSchema(currentVersion.getJsonSchemaPath());
		} else {
			this.jsonSchema = null;
		}
	}

	public YamlPlanReader() {
		this(null, null);
	}

	/**
	 * Read the plan from Yaml
	 *
	 * @param yamlPlan yaml data
	 */
	public Plan readYamlPlan(InputStream yamlPlan) throws IOException {
		String bufferedYamlPlan = new String(yamlPlan.readAllBytes(), StandardCharsets.UTF_8);

		bufferedYamlPlan = upgradeYamlPlanIfRequired(bufferedYamlPlan);

		JsonNode simplePlanJsonNode = yamlMapper.readTree(bufferedYamlPlan);
		if (jsonSchema != null) {
			JsonSchemaValidator.validate(jsonSchema, simplePlanJsonNode.toString());
		}

		YamlPlan simplePlan = yamlMapper.treeToValue(simplePlanJsonNode, YamlPlan.class);
		return yamlPlanToPlan(simplePlan);
	}

	/**
	 * Write the full plan as technical YAML (full serialization)
	 */
	public void writePlanInTechnicalFormat(OutputStream os, Plan plan) throws IOException {
		technicalPlanMapper.writeValue(os, plan);
	}

	/**
	 * Write the plan as YAML
	 */
	public void writeYamlPlan(OutputStream os, Plan plan) throws IOException {
		yamlMapper.writeValue(os, planToYamlPlan(plan));
	}

	protected ObjectMapper createYamlPlanObjectMapper() {
		YAMLFactory yamlFactory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		ObjectMapper yamlMapper = DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);

		// configure custom deserializers
		SimpleModule module = new SimpleModule();
		module.addDeserializer(DynamicValue.class, new YamlDynamicValueDeserializer());
		module.addDeserializer(YamlRootArtefact.class, createRootArtefactDeserializer());

		module.addSerializer(DynamicValue.class, new YamlDynamicValueSerializer());
		module.addSerializer(YamlRootArtefact.class, createRootArtefactSerializer());
		yamlMapper.registerModule(module);
		return yamlMapper;
	}

	protected YamlRootArtefactSerializer createRootArtefactSerializer() {
		return new YamlRootArtefactSerializer();
	}

	protected YamlRootArtefactDeserializer createRootArtefactDeserializer() {
		return new YamlRootArtefactDeserializer();
	}

	protected ObjectMapper createTechnicalPlanObjectMapper(){
		YAMLFactory yamlFactory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		return DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);
	}

	protected ObjectMapper getYamlMapper() {
		return yamlMapper;
	}

	protected ObjectMapper getTechnicalPlanMapper() {
		return technicalPlanMapper;
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
	 * Initialized the migration manager with specific migrations used for simple plan format
	 */
	protected MigrationManager initMigrationManager() {
		final MigrationManager migrationManager = new MigrationManager();
		Reflections migrationsReflections = new Reflections("step.plans");
		Set<Class<? extends AbstractYamlPlanMigrationTask>> migrations = migrationsReflections.getSubTypesOf(AbstractYamlPlanMigrationTask.class);
		for (Class<? extends AbstractYamlPlanMigrationTask> migration : migrations) {
			migrationManager.register(migration);
		}
		return migrationManager;
	}

	private String upgradeYamlPlanIfRequired(String bufferedYamlPlan) throws JsonProcessingException {
		if (currentVersion != null) {
			Document simplePlanDocument = yamlMapper.readValue(bufferedYamlPlan, Document.class);
			String planVersionString = simplePlanDocument.getString(YamlPlan.VERSION_FIELD_NAME);

			// planVersionString == null means than no migration is required (version is actual)
			if (planVersionString != null) {
				log.info("Migrating simple plan from version {} to {}", planVersionString, currentVersion);

				// convert yaml plan to document to perform migrations
				CollectionFactory tempCollectionFactory = new InMemoryCollectionFactory(new Properties());
				Version planVersion = new Version(planVersionString);

				if (planVersion.compareTo(currentVersion) != 0) {
					Collection<Document> tempCollection = tempCollectionFactory.getCollection(YAML_PLANS_COLLECTION_NAME, Document.class);
					Document planDocument = tempCollection.save(simplePlanDocument);

					// run migrations (AbstractSimplePlanMigrationTask)
					migrationManager.migrate(tempCollectionFactory, planVersion, currentVersion);

					Document migratedDocument = tempCollection.find(Filters.id(planDocument.getId()), null, null, null, 0).findFirst().orElseThrow();

					// remove automatically generated document id
					migratedDocument.remove(AbstractIdentifiableObject.ID);

					// convert document back to the yaml string
					bufferedYamlPlan = yamlMapper.writeValueAsString(migratedDocument);

					if (log.isDebugEnabled()) {
						log.debug("Simple plan after migrations: {}", bufferedYamlPlan);
					}
				}
			}
		}
		return bufferedYamlPlan;
	}

	protected Plan yamlPlanToPlan(YamlPlan yamlPlan) {
		Plan plan = new Plan(yamlPlan.getRoot().getAbstractArtefact());
		plan.addAttribute("name", yamlPlan.getName());
		applyDefaultValues(plan);
		return plan;
	}

	protected YamlPlan planToYamlPlan(Plan plan){
		YamlPlan yamlPlan = new YamlPlan();
		yamlPlan.setName(plan.getAttribute("name"));
		yamlPlan.setVersion(currentVersion.toString());
		yamlPlan.setRoot(new YamlRootArtefact(plan.getRoot()));
		return yamlPlan;
	}

	private void applyDefaultValues(Plan fullPlan) {
		if (this.idGenerator != null) {
			fullPlan.setId(this.idGenerator.get());
		}

		AbstractArtefact root = fullPlan.getRoot();
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
