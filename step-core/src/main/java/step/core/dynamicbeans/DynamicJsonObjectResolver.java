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
package step.core.dynamicbeans;

import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import step.core.json.JsonProviderCache;

public class DynamicJsonObjectResolver {
	
	DynamicJsonValueResolver valueResolver;
	
	public DynamicJsonObjectResolver(DynamicJsonValueResolver valueResolver) {
		super();
		this.valueResolver = valueResolver;
	}
	
	public JsonObject evaluate(JsonObject o, Map<String, Object> bindings) {
		JsonObjectBuilder builder = JsonProviderCache.createObjectBuilder();
		if(o!=null) {
			for(String key:o.keySet()) {
				JsonValue v = o.get(key);
				Object result = evaluateJsonValue(v, bindings);
				if(result instanceof JsonValue) {
					builder.add(key,(JsonValue)result);
				} else if(result instanceof Boolean) {
					builder.add(key,(Boolean)result);					
				} else if(result instanceof Integer) {
					builder.add(key,(Integer)result);
				} else if(result instanceof String) {
					builder.add(key,(String)result);
				} else {
					if(result != null)
						builder.add(key,(String)result.toString());
					else
						builder.add(key, ""); // a more restrictive try-catch of the NPE + throw "Value null for key={key}" might be more useful
				}
			}
		}
		return builder.build();
	}

	private Object evaluateJsonValue(JsonValue v, Map<String, Object> bindings) {
		if(v instanceof JsonObject) {
			JsonObject jsonObject = (JsonObject) v;
			if(jsonObject.containsKey("dynamic")) {
				return valueResolver.evaluate(jsonObject, bindings);
			} else {
				return evaluate(jsonObject, bindings);
			}
		} else if (v instanceof JsonArray) {
			JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
			JsonArray jsonArray = (JsonArray) v;
			for(int i=0;i<jsonArray.size();i++) {
				JsonValue jsonValue = jsonArray.get(i);
				Object o = evaluateJsonValue(jsonValue, bindings);
				if(o instanceof JsonValue) {
					arrayBuilder.add((JsonValue)o);
				} else if(o instanceof Boolean) {
					arrayBuilder.add((Boolean)o);					
				} else if(o instanceof Integer) {
					arrayBuilder.add((Integer)o);
				} else if(o instanceof String) {
					arrayBuilder.add((String)o);
				} else {
					arrayBuilder.add((String)o.toString());
				}
			}
			return arrayBuilder.build();
		} else {
			// in this case we have a primitive, so nothing to do
			return v;
		}
	}
}
