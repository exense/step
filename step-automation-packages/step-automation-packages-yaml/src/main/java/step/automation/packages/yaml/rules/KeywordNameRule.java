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
package step.automation.packages.yaml.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import step.automation.packages.yaml.deserialization.YamlKeywordFieldDeserializationProcessor;
import step.core.accessors.AbstractOrganizableObject;

import java.util.Map;

public class KeywordNameRule implements YamlKeywordConversionRule {

    @Override
    public YamlKeywordFieldDeserializationProcessor getDeserializationProcessor() {
        return new YamlKeywordFieldDeserializationProcessor() {
            @Override
            public boolean deserializeKeywordField(String keywordClass, Map.Entry<String, JsonNode> field, ObjectNode output, ObjectCodec codec) throws JsonProcessingException {
                if (field.getKey().equals(AbstractOrganizableObject.NAME)) {
                    ObjectNode attributesNode = (ObjectNode) output.get("attributes");
                    if (attributesNode == null) {
                        attributesNode = createObjectNode(codec);
                    }
                    attributesNode.put(AbstractOrganizableObject.NAME, field.getValue().asText());
                    output.set("attributes", attributesNode);
                    return true;
                } else {
                    return false;
                }
            }
        };
    }
}
