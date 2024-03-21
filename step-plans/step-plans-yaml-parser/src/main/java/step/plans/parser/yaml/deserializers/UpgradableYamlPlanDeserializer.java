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
package step.plans.parser.yaml.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.handlers.JsonSchemaValidator;
import step.core.Version;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.migration.MigrationManager;
import step.plans.parser.yaml.model.YamlPlan;
import step.plans.parser.yaml.schema.YamlPlanValidationException;

import java.io.IOException;
import java.util.Properties;

import static step.plans.parser.yaml.YamlPlanReader.YAML_PLANS_COLLECTION_NAME;

public class UpgradableYamlPlanDeserializer extends JsonDeserializer<YamlPlan> {

    private static final Logger log = LoggerFactory.getLogger(UpgradableYamlPlanDeserializer.class);
    private final Version currentVersion;
    private final MigrationManager migrationManager;
    private final ObjectMapper yamlMapper;
    private final String jsonSchema;

    public UpgradableYamlPlanDeserializer(Version currentVersion, String jsonSchema, MigrationManager migrationManager, ObjectMapper nonUpgradableYamlMapper) {
        this.currentVersion = currentVersion;
        this.jsonSchema = jsonSchema;
        this.migrationManager = migrationManager;
        this.yamlMapper = nonUpgradableYamlMapper;
    }

    @Override
    public YamlPlan deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode planJsonNode = p.readValueAsTree();

        if (currentVersion != null) {
            Document yamlPlanDocument = p.getCodec().treeToValue(planJsonNode, Document.class);
            String planVersionString = yamlPlanDocument.getString(YamlPlan.VERSION_FIELD_NAME);

            // planVersionString == null means than no migration is required (version is actual)
            if (planVersionString != null) {
                // convert yaml plan to document to perform migrations
                Version planVersion = new Version(planVersionString);

                if (planVersion.compareTo(currentVersion) != 0) {
                    CollectionFactory tempCollectionFactory = new InMemoryCollectionFactory(new Properties());

                    log.info("Migrating yaml plan from version {} to {}", planVersionString, currentVersion);

                    Collection<Document> tempCollection = tempCollectionFactory.getCollection(YAML_PLANS_COLLECTION_NAME, Document.class);
                    Document planDocument = tempCollection.save(yamlPlanDocument);

                    // run migrations (AbstractYamlPlanMigrationTask)
                    migrationManager.migrate(tempCollectionFactory, planVersion, currentVersion);

                    Document migratedDocument = tempCollection.find(Filters.id(planDocument.getId()), null, null, null, 0).findFirst().orElseThrow();

                    // set actual version
                    migratedDocument.replace(YamlPlan.VERSION_FIELD_NAME, currentVersion.toString());

                    // remove automatically generated document id
                    migratedDocument.remove(AbstractIdentifiableObject.ID);

                    // convert document back to the yaml string
                    String bufferedYamlPlan = yamlMapper.writeValueAsString(migratedDocument);

                    if (log.isDebugEnabled()) {
                        log.debug("Yaml plan after migrations: {}", bufferedYamlPlan);
                    }

                    planJsonNode = yamlMapper.readTree(bufferedYamlPlan);
                }
            }
        }

        if (jsonSchema != null) {
            try {
                JsonSchemaValidator.validate(jsonSchema, planJsonNode.toString());
            } catch (Exception ex) {
                throw new YamlPlanValidationException(ex.getMessage(), ex);
            }
        }

        return yamlMapper.treeToValue(planJsonNode, YamlPlan.class);
    }

}
