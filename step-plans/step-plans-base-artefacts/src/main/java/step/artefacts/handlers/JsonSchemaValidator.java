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
package step.artefacts.handlers;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.path.NodePath;
import com.networknt.schema.path.PathType;
import com.networknt.schema.serialization.DefaultNodeReader;
import com.networknt.schema.utils.JsonNodes;
import org.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonSchemaValidator {

    private static final SchemaRegistry REGISTRY =
        SchemaRegistry.builder(SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7))
            .nodeReader(DefaultNodeReader.Builder::locationAware)
            .build();


    public static void validate(String schemaJson, JsonNode nodeToValidate, JsonNode locationReferenceNode)
        throws JsonSchemaValidationException {

        try {
            Schema schema = REGISTRY.getSchema(schemaJson);
            List<Error> errors = schema.validate(nodeToValidate);

            if (errors.isEmpty()) return;

            throw new JsonSchemaValidationException(errors, getJsonLocationMappings(locationReferenceNode));

        } catch (JSONException e) {
            throw new RuntimeException("Error while validating input\n" + nodeToValidate.toString(), e);
        }
    }

    public static void validate(String schemaJson, String input)
        throws JsonSchemaValidationException {

        validate(schemaJson, input, InputFormat.YAML);
    }

    public static void validate(String schemaJson, String input, InputFormat inputFormat)
        throws JsonSchemaValidationException {

        try {

            Schema schema = REGISTRY.getSchema(schemaJson);
            List<Error> errors = schema.validate(input, inputFormat);

            errors = errors.stream().filter(e -> !e.getSchemaLocation().toString().equals("#/properties/version/const")).toList();

            if (errors.isEmpty()) return;

            throw new JsonSchemaValidationException(errors);

        } catch (JSONException e) {
            throw new RuntimeException("Error while validating input\n" + input, e);
        }
    }

    private static Map<NodePath, JsonLocation> getJsonLocationMappings(JsonNode locationReferenceNode) {
        Map<NodePath, JsonLocation> map = new HashMap<>();
        setJsonLocationMappings(locationReferenceNode, new NodePath(PathType.JSON_POINTER), map);
        return map;
    }

    private static void setJsonLocationMappings(JsonNode node, NodePath current, Map<NodePath, JsonLocation> map) {
        map.put(current, JsonNodes.tokenStreamLocationOf(node));
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> field : node.properties()) {
                setJsonLocationMappings(field.getValue(), current.append(field.getKey()), map);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                setJsonLocationMappings(node.get(i), current.append(i), map);
            }
        }
    }
}
