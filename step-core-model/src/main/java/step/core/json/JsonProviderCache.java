package step.core.json;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.spi.JsonProvider;

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
