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
package step.plans.simple;

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
import step.core.artefacts.Artefact;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.migration.MigrationManager;
import step.plans.simple.deserializers.SimpleDynamicValueDeserializer;
import step.plans.simple.deserializers.SimpleRootArtefactDeserializer;
import step.plans.simple.migrations.AbstractSimplePlanMigrationTask;
import step.plans.simple.model.SimpleRootArtefact;
import step.plans.simple.model.SimpleYamlPlan;
import step.plans.simple.model.SimpleYamlPlanVersions;
import step.plans.simple.serializers.SimpleDynamicValueSerializer;
import step.plans.simple.serializers.SimpleRootArtefactSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

public class YamlPlanSerializer {

	private static final Logger log = LoggerFactory.getLogger(YamlPlanSerializer.class);

	public static final String SIMPLE_PLANS_COLLECTION_NAME = "simplePlans";

	private final ObjectMapper simpleYamlMapper;
	private final ObjectMapper fullYamlMapper;

	private final Supplier<ObjectId> idGenerator;
	private final Version currentVersion;
	private final String jsonSchema;
	private final MigrationManager migrationManager;


	// for tests
	YamlPlanSerializer( Supplier<ObjectId> idGenerator, SimpleYamlPlanVersions.YamlPlanVersion currentVersion) {
		this.currentVersion = currentVersion.getVersion();
		if (currentVersion.getJsonSchemaPath() != null) {
			this.jsonSchema = readJsonSchema(currentVersion.getJsonSchemaPath());
		} else {
			this.jsonSchema = null;
		}
		this.simpleYamlMapper = createSimplePlanObjectMapper();
		this.fullYamlMapper = createFullPlanObjectMapper();
		this.idGenerator = idGenerator;
		this.migrationManager = initMigrationManager();
	}

	/**
	 * @param currentVersion the current version of step used to upgrade the old simple plans and apply validations according to the json schema
	 */
	public YamlPlanSerializer(SimpleYamlPlanVersions.YamlPlanVersion currentVersion) {
		this(null, currentVersion);
	}

	public static ObjectMapper createSimplePlanObjectMapper() {
		YAMLFactory yamlFactory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		ObjectMapper yamlMapper = DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);

		// configure custom deserializers
		SimpleModule module = new SimpleModule();
		module.addDeserializer(DynamicValue.class, new SimpleDynamicValueDeserializer());
		module.addDeserializer(SimpleRootArtefact.class, new SimpleRootArtefactDeserializer());

		module.addSerializer(SimpleRootArtefact.class, new SimpleRootArtefactSerializer());
		module.addSerializer(DynamicValue.class, new SimpleDynamicValueSerializer());
		yamlMapper.registerModule(module);
		return yamlMapper;
	}

	public static ObjectMapper createFullPlanObjectMapper(){
		YAMLFactory yamlFactory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		return DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);
	}

	public static String getArtefactName(Class<?> artefactClass) {
		Artefact ann = artefactClass.getAnnotation(Artefact.class);
		return ann.name() == null || ann.name().isEmpty() ? artefactClass.getSimpleName() : ann.name();
	}

	public ObjectMapper getSimpleYamlMapper() {
		return simpleYamlMapper;
	}

	public ObjectMapper getFullYamlMapper() {
		return fullYamlMapper;
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
	private MigrationManager initMigrationManager() {
		final MigrationManager migrationManager = new MigrationManager();
		Reflections migrationsReflections = new Reflections("step.plans.simple");
		Set<Class<? extends AbstractSimplePlanMigrationTask>> migrations = migrationsReflections.getSubTypesOf(AbstractSimplePlanMigrationTask.class);
		for (Class<? extends AbstractSimplePlanMigrationTask> migration : migrations) {
			migrationManager.register(migration);
		}
		return migrationManager;
	}


	/**
	 * Read the plan from simplified yaml format
	 *
	 * @param planYaml yaml data
	 */
	public Plan readSimplePlanFromYaml(InputStream planYaml) throws IOException {
		String bufferedYamlPlan = new String(planYaml.readAllBytes(), StandardCharsets.UTF_8);

		bufferedYamlPlan = upgradeSimpleYamlIfRequired(bufferedYamlPlan);

		JsonNode simplePlanJsonNode = simpleYamlMapper.readTree(bufferedYamlPlan);
		if (jsonSchema != null) {
			JsonSchemaValidator.validate(jsonSchema, simplePlanJsonNode.toString());
		}

		SimpleYamlPlan simplePlan = simpleYamlMapper.treeToValue(simplePlanJsonNode, SimpleYamlPlan.class);
		return convertSimplePlanToFullPlan(simplePlan);
	}

	private String upgradeSimpleYamlIfRequired(String bufferedYamlPlan) throws JsonProcessingException {
		if (currentVersion != null) {
			Document simplePlanDocument = simpleYamlMapper.readValue(bufferedYamlPlan, Document.class);
			String planVersionString = simplePlanDocument.getString(SimpleYamlPlan.VERSION_FIELD_NAME);

			// planVersionString == null means than no migration is required (version is actual)
			if (planVersionString != null) {
				log.info("Migrating simple plan from version {} to {}", planVersionString, currentVersion);

				// convert yaml plan to document to perform migrations
				CollectionFactory tempCollectionFactory = new InMemoryCollectionFactory(new Properties());
				Version planVersion = new Version(planVersionString);

				if (planVersion.compareTo(currentVersion) != 0) {
					Collection<Document> tempCollection = tempCollectionFactory.getCollection(SIMPLE_PLANS_COLLECTION_NAME, Document.class);
					Document planDocument = tempCollection.save(simplePlanDocument);

					// run migrations (AbstractSimplePlanMigrationTask)
					migrationManager.migrate(tempCollectionFactory, planVersion, currentVersion);

					Document migratedDocument = tempCollection.find(Filters.id(planDocument.getId()), null, null, null, 0).findFirst().orElseThrow();

					// remove automatically generated document id
					migratedDocument.remove(AbstractIdentifiableObject.ID);

					// convert document back to the yaml string
					bufferedYamlPlan = simpleYamlMapper.writeValueAsString(migratedDocument);

					if (log.isDebugEnabled()) {
						log.debug("Simple plan after migrations: {}", bufferedYamlPlan);
					}
				}
			}
		}
		return bufferedYamlPlan;
	}

	protected Plan convertSimplePlanToFullPlan(SimpleYamlPlan simpleYamlPlan) {
		Plan fullPlan = new Plan(simpleYamlPlan.getRoot().getAbstractArtefact());
		fullPlan.addAttribute("name", simpleYamlPlan.getName());
		applyDefaultValues(fullPlan);
		return fullPlan;
	}

	protected SimpleYamlPlan convertFullPlanToSimplePlan(Plan plan){
		SimpleYamlPlan simplePlan = new SimpleYamlPlan();
		simplePlan.setName(plan.getAttribute("name"));
		simplePlan.setVersion(currentVersion.toString());
		simplePlan.setRoot(new SimpleRootArtefact(plan.getRoot()));
		return simplePlan;
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

	/**
	 * Write the full plan as YAML
	 */
	public void writeFullYaml(OutputStream os, Plan plan) throws IOException {
		fullYamlMapper.writeValue(os, plan);
	}

	/**
	 * Write the simple plan as YAML
	 */
	public void writeSimpleYaml(OutputStream os, SimpleYamlPlan plan) throws IOException {
		simpleYamlMapper.writeValue(os, plan);
	}

}
