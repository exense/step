package step.artefacts.handlers;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonSchemaValidator {

	public static void validate(String schema, String input) {
		try {
			JSONObject jsonSchema = new JSONObject(schema);
			JSONObject jsonSubject = new JSONObject(input);
			
			Schema schema_ = SchemaLoader.load(jsonSchema);
			schema_.validate(jsonSubject);
		} catch (JSONException e) {
			throw new RuntimeException("Error while validating input " + input + " using schema "+schema, e);
		}
	}
}
