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
package step.core.yaml.deserializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class NamedEntityYamlDeserializer<T>  {

    public NamedEntityYamlDeserializer() {
    }

    public T deserialize(JsonNode namedEntity, ObjectCodec codec) throws IOException {
        ObjectNode techYaml = namedEntityToTechYaml(namedEntity, codec);
        String yamlName = getEntityNameFromYaml(namedEntity);
        return (T) codec.treeToValue(techYaml, resolveTargetClassByYamlName(yamlName));
    }

    private ObjectNode namedEntityToTechYaml(JsonNode namedEntity, ObjectCodec codec) throws JsonProcessingException {
        ObjectNode techYaml = createObjectNode(codec);

        String yamlName = getEntityNameFromYaml(namedEntity);

        String targetClass = resolveTargetClassNameByYamlName(yamlName);

        // move entity name into the target '_class' field
        JsonNode allYamlFields = namedEntity.get(yamlName);

        String targetClassField = getTargetClassField();
        if (targetClassField != null) {
            if (targetClass == null) {
                throw new RuntimeException("Unable to resolve implementation class for entity " + yamlName);
            }
            techYaml.put(targetClassField, targetClass);
        }

        Iterator<Map.Entry<String, JsonNode>> fields = allYamlFields.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();

            // copy all other fields
            techYaml.set(next.getKey(), next.getValue().deepCopy());
        }
        return techYaml;
    }

    public JsonNode getAllYamlFields(JsonNode node){
        String yamlName = getEntityNameFromYaml(node);

        // move entity name into the target '_class' field
        return node.get(yamlName);
    }

    protected String resolveTargetClassNameByYamlName(String yamlName)  {
        Class<?> clazz = resolveTargetClassByYamlName(yamlName);
        return clazz == null ? null : clazz.getName();
    }

    protected abstract Class<?> resolveTargetClassByYamlName(String yamlName);

    protected String getEntityNameFromYaml(JsonNode yamlNode) {
        Iterator<String> nameIterator = yamlNode.fieldNames();

        List<String> names = new ArrayList<String>();
        nameIterator.forEachRemaining(names::add);

        String yamlName = null;
        if (names.size() == 0) {
            throw new RuntimeException("Entity should have a name");
        } else if (names.size() > 1) {
            throw new RuntimeException("Entity should have only one name");
        } else {
            yamlName = names.get(0);
        }

        if (yamlName == null) {
            throw new RuntimeException("Entity class cannot be resolved");
        }
        return yamlName;
    }

    protected String getTargetClassField(){
        return null;
    }

    private static ObjectNode createObjectNode(ObjectCodec codec) {
        return (ObjectNode) codec.createObjectNode();
    }
}
