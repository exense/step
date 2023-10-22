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
package step.automation.packages.yaml.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.spi.JsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Version;
import step.handlers.javahandler.jsonschema.*;
import step.plans.parser.yaml.model.YamlPlanVersions;
import step.plans.parser.yaml.schema.YamlPlanJsonSchemaGenerator;

public class YamlAutomationPackageSchemaGenerator {
    private static final Logger log = LoggerFactory.getLogger(YamlAutomationPackageSchemaGenerator.class);

    protected final String targetPackage;

    protected final Version actualVersion;

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected final JsonProvider jsonProvider = JsonProvider.provider();
    private final YamlKeywordSchemaGenerator keywordSchemaGenerator;
    private final YamlPlanJsonSchemaGenerator planSchemaGenerator;

    public YamlAutomationPackageSchemaGenerator(String targetPackage, Version actualVersion) {
        this.targetPackage = targetPackage;
        this.actualVersion = actualVersion;
        this.keywordSchemaGenerator = new YamlKeywordSchemaGenerator(jsonProvider);
        this.planSchemaGenerator = new YamlPlanJsonSchemaGenerator("step", YamlPlanVersions.ACTUAL_VERSION, null);
    }

    public JsonNode generateJsonSchema() throws JsonSchemaPreparationException {
        JsonObjectBuilder topLevelBuilder = jsonProvider.createObjectBuilder();

        // common fields for json schema
        topLevelBuilder.add("$schema", "http://json-schema.org/draft-07/schema#");
        topLevelBuilder.add("title", "Step Automation Package");
        topLevelBuilder.add("type", "object");

        // prepare definitions to be reused in subschemas (referenced via $ref property)
        topLevelBuilder.add("$defs", keywordSchemaGenerator.createKeywordDefs().addAll(planSchemaGenerator.createDefs()));

        // add properties for top-level
        topLevelBuilder.add("properties", createPackageProperties());
        topLevelBuilder.add("required", jsonProvider.createArrayBuilder());
        topLevelBuilder.add( "additionalProperties", false);

        // convert jakarta objects to jackson JsonNode
        try {
            return fromJakartaToJsonNode(topLevelBuilder);
        } catch (JsonProcessingException e) {
            throw new JsonSchemaPreparationException("Unable to convert json to jackson jsonNode", e);
        }
    }

    private JsonObjectBuilder createPackageProperties() {
        JsonObjectBuilder objectBuilder = jsonProvider.createObjectBuilder();
        // in 'version' we should either explicitly specify the current json schema version or skip this field
        objectBuilder.add("version", jsonProvider.createObjectBuilder().add("const", actualVersion.toString()));
        objectBuilder.add("name", jsonProvider.createObjectBuilder().add("type", "string"));

        // TODO: split keyword and plans definitions
        objectBuilder.add("keywords",
                jsonProvider.createObjectBuilder()
                        .add("type", "array")
                        .add("items", keywordSchemaGenerator.addRef(jsonProvider.createObjectBuilder(), YamlKeywordSchemaGenerator.KEYWORD_DEF)));

        objectBuilder.add("plans",
                jsonProvider.createObjectBuilder()
                        .add("type", "array")
                        .add("items", jsonProvider.createObjectBuilder()
                                .add("type", "object")
                                .add("properties", planSchemaGenerator.createYamlPlanProperties()))
                                .add("required", jsonProvider.createArrayBuilder().add("name").add("root"))
        );
        return objectBuilder;
    }

    private JsonNode fromJakartaToJsonNode(JsonObjectBuilder objectBuilder) throws JsonProcessingException {
        return objectMapper.readTree(objectBuilder.build().toString());
    }


}
