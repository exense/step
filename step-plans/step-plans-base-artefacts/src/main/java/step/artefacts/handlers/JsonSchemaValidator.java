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

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonSchemaValidator {

	public static String validate(String schema, String input) {
		try {
			JSONObject jsonSchema = new JSONObject(schema);
			JSONObject jsonSubject = new JSONObject(input);
			
			Schema schema_ = SchemaLoader.load(jsonSchema);
			schema_.validate(jsonSubject);
			try {
				return jsonSubject.getString("schemaVersion");
			} catch (Exception e) {
				return null;
			}
		} catch (JSONException e) {
			throw new RuntimeException("Error while validating input \n" + input, e);
		}
	}
}
