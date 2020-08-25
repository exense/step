/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.core.execution.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ExecutionParameterMapSerializer extends JsonSerializer<Map<String, String>> {

	public ExecutionParameterMapSerializer() {
		super();
	}

	public class MapEntry {
		
		private String key;
		
		private String value;

		public MapEntry(String key, String value) {
			super();
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}
	}
	
	@Override
	public void serialize(Map<String, String> value, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
			JsonProcessingException {		
		List<MapEntry> entries = new ArrayList<>();
		for(String key:value.keySet()) {
			String entryValue;
			if(key.trim().contains("pwd")||key.trim().contains("password")) {
				entryValue = "*****";
			} else {
				entryValue = value.get(key);
			}
			entries.add(new MapEntry(key, entryValue));
		}
		jgen.writeObject(entries);
	}
}
