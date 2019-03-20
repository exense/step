package step.artefacts.handlers;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonParsingException;

import step.core.dynamicbeans.DynamicJsonObjectResolver;

public class SelectorHelper {

	private static JsonProvider jprov = JsonProvider.provider();
	
	protected DynamicJsonObjectResolver dynamicJsonObjectResolver;
	
	public SelectorHelper(DynamicJsonObjectResolver dynamicJsonObjectResolver) {
		super();
		this.dynamicJsonObjectResolver = dynamicJsonObjectResolver;
	}
	
	private JsonObject parseAndResolveJson(String jsonStr, Map<String, Object> bindings) {
		JsonObject query;
		try {
			if(jsonStr!=null&&jsonStr.trim().length()>0) {
				query = jprov.createReader(new StringReader(jsonStr)).readObject();
			} else {
				query = jprov.createObjectBuilder().build();
			}
		} catch(JsonParsingException e) {
			throw new RuntimeException("Error while parsing json "+jsonStr+" :"+e.getMessage());
		}
		return dynamicJsonObjectResolver.evaluate(query, bindings);
	}
	
	public Map<String, String> buildSelectionAttributesMap(String jsonStr, Map<String, Object> bindings) {
		JsonObject json = parseAndResolveJson(jsonStr, bindings);
		Map<String, String> attributes = new HashMap<>();
		json.forEach((key,value)->attributes.put(key, json.getString(key)));
		return attributes;
	}
}
