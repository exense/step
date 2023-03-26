package step.functions.packages.handlers;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParsingException;
import step.handlers.javahandler.JsonSchema;
import step.handlers.javahandler.Keyword;

import java.io.StringReader;

public class KeywordJsonSchemaReader {

	public JsonObject readJsonSchemaForKeyword(Keyword keywordAnnotation, String methodName) throws JsonSchemaPreparationException{
		boolean useTextJsonSchema = keywordAnnotation.schema() != null && !keywordAnnotation.schema().isEmpty();
		boolean useAnnotatedJsonSchema = keywordAnnotation.jsonSchema() != null && keywordAnnotation.jsonSchema().length > 0;
		if (useTextJsonSchema && useAnnotatedJsonSchema) {
			throw new IllegalArgumentException("Ambiguous definition of json schema for keyword. You should define either 'jsonSchema' or 'schema' parameter in @Keyword annotation");
		}

		if (useTextJsonSchema) {
			return readJsonSchemaFromPlainText(keywordAnnotation.schema(), methodName);
		} else if (useAnnotatedJsonSchema) {
			return readJsonSchemaFromAnnotationKeyword(keywordAnnotation.jsonSchema()[0]);
		} else {
			return createEmptyJsonSchema();
		}
	}

	private JsonObject readJsonSchemaFromAnnotationKeyword(JsonSchema jsonSchema) {
		// TODO: build the json schema
		return Json.createObjectBuilder().build();
	}

	protected JsonObject createEmptyJsonSchema() {
		return Json.createObjectBuilder().build();
	}

	protected JsonObject readJsonSchemaFromPlainText(String schema, String methodName) throws JsonSchemaPreparationException {
		try {
			return Json.createReader(new StringReader(schema)).readObject();
		} catch (JsonParsingException e) {
			throw new JsonSchemaPreparationException("Parsing error in the schema for keyword '" + methodName + "'. The error was: " + e.getMessage());
		} catch (JsonException e) {
			throw new JsonSchemaPreparationException("I/O error in the schema for keyword '" + methodName + "'. The error was: " + e.getMessage());
		} catch (Exception e) {
			throw new JsonSchemaPreparationException("Unknown error in the schema for keyword '" + methodName + "'. The error was: " + e.getMessage());
		}
	}
}
