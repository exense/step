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
package step.datapool.json;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.*;

import step.datapool.DataSet;

public class JsonArrayDataPoolImpl extends DataSet<JsonArrayDataPoolConfiguration> {

	private final JsonArray array;

	private int cursor = 0;

	public JsonArrayDataPoolImpl(JsonArrayDataPoolConfiguration configuration) {
		super(configuration);

		JsonReader reader = Json.createReader(new StringReader(configuration.getJson().get()));
		try {
			array = reader.readArray();
		} catch (Exception e) {
			throw new RuntimeException("Error while parsing JSON. " + expectedFormat(), e);
		}

	}

	@Override
	public void reset() {
		cursor = 0;
	}

	@Override
	public Object next_() {
		if (cursor < array.size()) {
			JsonValue jsonValue = array.get(cursor);
			Map<String, String> row = new HashMap<>();
			if (jsonValue instanceof JsonObject) {
				JsonObject jsonObject = (JsonObject) jsonValue;
				jsonObject.forEach((key, value) -> {
					String strValue = (value instanceof JsonString) ? ((JsonString) value).getString() : value.toString();
					row.put(key, strValue);
				});
			} else {
				throw new RuntimeException("Unexpected value in row " + cursor + ". " + expectedFormat());
			}
			cursor++;
			return row;
		} else {
			return null;
		}
	}

	private String expectedFormat() {
		return "Expected format is [{\"MyKey1\": \"Value 1\", \"MyKey2\": \"Value 2\"}, {...}]";
	}

	@Override
	public void addRow(Object row) {
		throw new RuntimeException("Not implemented");
	}
}
