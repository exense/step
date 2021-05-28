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
package step.core.accessors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr353.JSR353Module;

public class DefaultJacksonMapperProvider {

	private static List<Module> customModules = new ArrayList<>();
	private static List<Module> modules = new ArrayList<>();
	private static Class<ObjectId> _id = ObjectId.class;
	
	static {
		customModules.add(new JSR353Module());
		customModules.add(new JsonOrgModule());
        modules.add(new SimpleModule("jersey", new Version(1, 0, 0, null,null,null)) //
                .addSerializer(_id, _idSerializer()) //
                .addDeserializer(_id, _idDeserializer()));
	}

	public static List<Module> getCustomModules() {
		return customModules;
	}
	
	public static ObjectMapper getObjectMapper(JsonFactory factory) {
		ObjectMapper objectMapper = new ObjectMapper(factory);
		configure(objectMapper);
		return objectMapper;
	}

	public static ObjectMapper getObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		configure(objectMapper);
		return objectMapper;
	}

	private static void configure(ObjectMapper objectMapper) {
		customModules.forEach(m->objectMapper.registerModule(m));
		modules.forEach(m->objectMapper.registerModule(m));
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
    private static JsonDeserializer<ObjectId> _idDeserializer() {
        return new JsonDeserializer<ObjectId>() {
            public ObjectId deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return new ObjectId(jp.readValueAs(String.class));
            }
        };
    }
 
    private static JsonSerializer<Object> _idSerializer() {
        return new JsonSerializer<Object>() {
            public void serialize(Object obj, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException, JsonProcessingException {
                jsonGenerator.writeString(obj == null ? null : obj.toString());
            }
        };
    }
}
