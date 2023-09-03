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
package step.functions.packages.yaml.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import step.functions.packages.yaml.model.YamlKeywords;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class YamlKeywordsDeserializer  extends JsonDeserializer<YamlKeywords> {

    private File unzippedAutomationPackage;

    public YamlKeywordsDeserializer(File unzippedAutomationPackage) {
        this.unzippedAutomationPackage = unzippedAutomationPackage;
    }

    @Override
    public YamlKeywords deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        if(node.isArray()){
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode keywordNode : arrayNode) {
                Iterator<String> keywordNameIterator = keywordNode.fieldNames();
                if(keywordNameIterator.hasNext()){
                    String keywordName = keywordNameIterator.next();
                    // TODO: resolve via annotation
                    if("JMeter".equalsIgnoreCase(keywordName)){

                    }
                }
            }
        }
        return new YamlKeywords();
    }
}
