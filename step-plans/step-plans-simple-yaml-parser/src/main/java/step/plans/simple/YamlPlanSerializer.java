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

	private final ObjectMapper yamlMapper;

	private final Supplier<ObjectId> idGenerator;
	private InputStream jsonSchemaFile = null;
	private final Version currentVersion;
	private final MigrationManager migrationManager;


	// for tests
	YamlPlanSerializer(InputStream jsonSchemaFile, Supplier<ObjectId> idGenerator, Version currentVersion) {
		this.jsonSchemaFile = jsonSchemaFile;
		this.currentVersion = currentVersion;
		this.yamlMapper = createSimplePlanObjectMapper();
		this.idGenerator = idGenerator;
		this.migrationManager = initMigrationManager();
	}

	/**
	 * @param jsonSchemaFile the json schema used to validate the simple yaml plan (if null, no validations are performed)
	 * @param currentVersion the current version of step used to upgrade the old simple plans (if null, no migrations are performed)
	 */
	public YamlPlanSerializer(InputStream jsonSchemaFile, Version currentVersion) {
		this(jsonSchemaFile, null, currentVersion);
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

	public static ObjectMapper createSimplePlanObjectMapper() {
		YAMLFactory yamlFactory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		ObjectMapper yamlMapper = DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);

		// configure custom deserializers
		SimpleModule module = new SimpleModule();
		module.addDeserializer(DynamicValue.class, new SimpleDynamicValueDeserializer());
		module.addDeserializer(SimpleRootArtefact.class, new SimpleRootArtefactDeserializer());
		yamlMapper.registerModule(module);
		return yamlMapper;
	}

	/**
	 * Read the plan from simplified yaml format
	 *
	 * @param planYaml yaml data
	 */
	public Plan readSimplePlanFromYaml(InputStream planYaml) throws IOException {
		String bufferedYamlPlan = new String(planYaml.readAllBytes(), StandardCharsets.UTF_8);

		bufferedYamlPlan = upgradeSimpleYamlIfRequired(bufferedYamlPlan);

		JsonNode simplePlanJsonNode = yamlMapper.readTree(bufferedYamlPlan);
		if (jsonSchemaFile != null) {
			String jsonSchema = new String(jsonSchemaFile.readAllBytes(), StandardCharsets.UTF_8);
			JsonSchemaValidator.validate(jsonSchema, simplePlanJsonNode.toString());
		}

		SimpleYamlPlan simplePlan = yamlMapper.treeToValue(simplePlanJsonNode, SimpleYamlPlan.class);
		return convertSimplePlanToFullPlan(simplePlan);
	}

	private String upgradeSimpleYamlIfRequired(String bufferedYamlPlan) throws JsonProcessingException {
		if (currentVersion != null) {
			Document simplePlanDocument = yamlMapper.readValue(bufferedYamlPlan, Document.class);
			String planVersionString = simplePlanDocument.getString(SimpleYamlPlan.VERSION_FIELD_NAME);

			// planVersionString == null means than no migration is required (version is actual)
			if (planVersionString != null) {
				log.info("Migrating simple plan from version {} to {}", planVersionString, currentVersion);

				// convert yaml plan to document to perform migrations
				CollectionFactory tempCollectionFactory = new InMemoryCollectionFactory(new Properties());
				Version planVersion = new Version(planVersionString);
				Collection<Document> tempCollection = tempCollectionFactory.getCollection(SIMPLE_PLANS_COLLECTION_NAME, Document.class);
				Document planDocument = tempCollection.save(simplePlanDocument);

				// run migrations (AbstractSimplePlanMigrationTask)
				migrationManager.migrate(tempCollectionFactory, planVersion, currentVersion);

				Document migratedDocument = tempCollection.find(Filters.id(planDocument.getId()), null, null, null, 0).findFirst().orElseThrow();

				// remove automatically generated document id
				migratedDocument.remove(AbstractIdentifiableObject.ID);

				// convert document back to the yaml string
				bufferedYamlPlan = yamlMapper.writeValueAsString(migratedDocument);

				if(log.isDebugEnabled()){
					log.debug("Simple plan after migrations: {}", bufferedYamlPlan);
				}
			}
		}
		return bufferedYamlPlan;
	}

	public ObjectMapper getYamlMapper() {
		return yamlMapper;
	}

	private Plan convertSimplePlanToFullPlan(SimpleYamlPlan simpleYamlPlan) {
		Plan fullPlan = new Plan(simpleYamlPlan.getRoot().getAbstractArtefact());
		fullPlan.addAttribute("name", simpleYamlPlan.getName());
		applyDefaultValues(fullPlan);
		return fullPlan;
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
	public void toFullYaml(OutputStream os, Plan plan) throws IOException {
		yamlMapper.writeValue(os, plan);
	}

}
