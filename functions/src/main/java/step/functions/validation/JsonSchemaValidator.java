package step.functions.validation;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

public class JsonSchemaValidator {

	public static void validate(String schema, String input){
		JSONObject jsonSchema = new JSONObject(schema);
		JSONObject jsonSubject = new JSONObject(input);
		
	    Schema schema_ = SchemaLoader.load(jsonSchema);
	    schema_.validate(jsonSubject);
	}
}
