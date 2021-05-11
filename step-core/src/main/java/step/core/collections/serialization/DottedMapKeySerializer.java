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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

@SuppressWarnings("rawtypes")
public class DottedMapKeySerializer extends JsonSerializer<DottedKeyMap> {

	public DottedMapKeySerializer() {
		super();
	}

	@Override
	public void serialize(DottedKeyMap value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		Map<Object, Object> newMap = new HashMap<>();
		for (Object key : value.keySet()) {
			newMap.put(encodeKey(key), value.get(key));
		}
		ObjectCodec initialCodec = jgen.getCodec();
		jgen.setCodec(new ObjectMapper());
		try {
			jgen.writeObject(newMap);
		} finally {
			jgen.setCodec(initialCodec);
		}
	}

	// replacing "." and "$" by their unicodes as they are invalid keys in BSON
	private Object encodeKey(Object key) {
		if (key instanceof String) {
			return ((String) key).replace("\\\\", "\\\\\\\\").replace("\\$", "\\\\u0024").replace(".", "\\\\u002e");
		} else {
			return key;
		}
	}

}
