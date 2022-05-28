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
package step.core.json;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.json.spi.JsonProvider;

/**
 * Calls to {@link JsonProvider#provider()} and thus {@link Json#createObjectBuilder()}
 * which is relying on it are very inefficient. The {@link JsonProvider} has
 * therefore to be cached.
 * 
 * This class should be used everywhere instead of calling {@link Json} directly
 *
 */
public class JsonProviderCache {

	public static JsonProvider JSON_PROVIDER  = JsonProvider.provider();

	public static JsonObjectBuilder createObjectBuilder() {
		return JSON_PROVIDER.createObjectBuilder();
	}

	public static JsonArrayBuilder createArrayBuilder() {
		return JSON_PROVIDER.createArrayBuilder();
	}

	public static JsonReader createReader(Reader reader) {
		return JSON_PROVIDER.createReader(reader);
	}

	public static JsonReader createReader(InputStream in) {
		return JSON_PROVIDER.createReader(in);
	}

	public static JsonWriter createWriter(Writer writer) {
		return JSON_PROVIDER.createWriter(writer);
	}

	public static JsonWriter createWriter(OutputStream out) {
		return JSON_PROVIDER.createWriter(out);
	}
}
