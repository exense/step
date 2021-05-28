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
package step.core.collections.mongodb;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.serialization.DottedKeyMap;
import step.core.collections.serialization.DottedMapKeyDeserializer;
import step.core.collections.serialization.DottedMapKeySerializer;

class MongoDBCollectionJacksonMapperProvider {

	public static List<Module> modules = new ArrayList<>();

	static {
		modules.addAll(DefaultJacksonMapperProvider.getCustomModules());
		modules.add(new DefaultAccessorModule());
	}

	public static ObjectMapper getObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		modules.forEach(m -> objectMapper.registerModule(m));
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return objectMapper;
	}

	private static class DefaultAccessorModule extends SimpleModule {

		private static final long serialVersionUID = 5544301456563146100L;

		public DefaultAccessorModule() {
			super();
			// MongoDB doesn't support keys with dots in documents. The following
			// serializers are responsible for escaping dots in keys to be able to store it
			// in MongoDB
			addSerializer(DottedKeyMap.class, new DottedMapKeySerializer());
			addDeserializer(DottedKeyMap.class, new DottedMapKeyDeserializer());
		}
	}

}
