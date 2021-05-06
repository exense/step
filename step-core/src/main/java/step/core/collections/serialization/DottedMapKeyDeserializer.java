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
package step.core.collections.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings("rawtypes")
public class DottedMapKeyDeserializer extends JsonDeserializer<DottedKeyMap> {

	private ObjectMapper mapper;
	
    public DottedMapKeyDeserializer() {
		super();
		mapper = new ObjectMapper();
		mapper.enableDefaultTyping();
	}

	private String decodeKey(String key) {
        return key.replace("\\\\u002e", ".").replace("\\\\u0024", "\\$").replace("\\\\\\\\", "\\\\");
    }

	@Override
	public DottedKeyMap deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode node = jp.readValueAsTree();
		
		DottedKeyMap result = new DottedKeyMap<>();
		ObjectNode o = (ObjectNode) node;
        o.fields().forEachRemaining(e -> {
        	String key = e.getKey();
        	JsonNode eNode = e.getValue();
        	try {
        		result.put(decodeKey(key), mapper.treeToValue(eNode, Object.class));
        	} catch (Throwable ex) {
        		// Ignore these exception as it can be a ClassNotFoundException
        	}
        });
		
		return result;
	}
}
