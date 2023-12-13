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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Deserializes the field from yaml format to the technical format
 */
public interface YamlFieldDeserializationProcessor {

    /**
     * @param entityClassName the name (class) of entity
     * @param field the field name/value in yaml format
     * @param output the output json node (in technical format)
     * @param codec the codec to create json elements
     * @return true if this processor is applicable for the keyword field, false otherwise
     */
    boolean deserializeField(String entityClassName, Map.Entry<String, JsonNode> field, ObjectNode output, ObjectCodec codec) throws JsonProcessingException;

    default ArrayNode createArrayNode(ObjectCodec codec) {
        return (ArrayNode) codec.createArrayNode();
    }

    default ObjectNode createObjectNode(ObjectCodec codec) {
        return (ObjectNode) codec.createObjectNode();
    }
}
