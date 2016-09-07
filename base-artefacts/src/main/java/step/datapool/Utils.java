package step.datapool;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class Utils {

	public static JsonObject toJson(Map<String, String> map) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		for(String key:map.keySet()) {
			builder.add(key, map.get(key));
		}
		return builder.build();
	}
}
