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
package step.artefacts.handlers;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParsingException;

import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.json.JsonProviderCache;

public class SelectorHelper {

	protected DynamicJsonObjectResolver dynamicJsonObjectResolver;
	
	public SelectorHelper(DynamicJsonObjectResolver dynamicJsonObjectResolver) {
		super();
		this.dynamicJsonObjectResolver = dynamicJsonObjectResolver;
	}
	
	private JsonObject parseAndResolveJson(String jsonStr, Map<String, Object> bindings) {
		JsonObject query;
		try {
			if(jsonStr!=null&&jsonStr.trim().length()>0) {
				query = JsonProviderCache.createReader(new StringReader(jsonStr)).readObject();
			} else {
				query = JsonProviderCache.createObjectBuilder().build();
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
