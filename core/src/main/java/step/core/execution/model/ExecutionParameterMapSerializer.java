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
