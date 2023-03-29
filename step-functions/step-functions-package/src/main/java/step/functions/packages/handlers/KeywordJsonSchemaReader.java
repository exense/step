package step.functions.packages.handlers;

import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;
import step.handlers.javahandler.JsonSchema;
import step.handlers.javahandler.JsonSchemaProperty;
import step.handlers.javahandler.JsonSchemaPropertyRef;
import step.handlers.javahandler.Keyword;

import java.io.StringReader;
import java.util.*;

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
			return readJsonSchemaFromAnnotationKeyword(keywordAnnotation.jsonSchema()[0], methodName);
		} else {
			return createEmptyJsonSchema();
		}
	}

	private JsonObject readJsonSchemaFromAnnotationKeyword(JsonSchema jsonSchema, String methodName) throws JsonSchemaPreparationException {
		Map<String, JsonSchemaProperty> propertyRefsRegistry = new HashMap<>();
		for (JsonSchemaPropertyRef jsonSchemaPropertyRef : jsonSchema.propertiesRefs()) {
			propertyRefsRegistry.put(jsonSchemaPropertyRef.id(), jsonSchemaPropertyRef.property());
		}

		JsonObjectBuilder topLevelBuilder = Json.createObjectBuilder();

		if (jsonSchema.title() != null && !jsonSchema.title().isEmpty()) {
			topLevelBuilder.add("title", jsonSchema.title());
		}
		if (jsonSchema.description() != null && !jsonSchema.description().isEmpty()) {
			topLevelBuilder.add("description", jsonSchema.description());
		}
		JsonArrayBuilder propertiesBuilder = Json.createArrayBuilder();
		topLevelBuilder.add("properties", propertiesBuilder);
		List<String> requiredProperties = new ArrayList<>();
		for (JsonSchemaProperty property : jsonSchema.properties()) {
			JsonObjectBuilder propertyBuilder = Json.createObjectBuilder();
			String ref = property.propertyRef();
			if(ref != null && !ref.isEmpty()){
				property = getPropertyByRef(ref, propertyRefsRegistry, methodName);
			}

			JsonObjectBuilder propertyParamsBuilder = Json.createObjectBuilder();

			propertyBuilder.add(property.name(), propertyParamsBuilder);

			if (property.type() == null || property.type().isEmpty()) {
				throw new JsonSchemaPreparationException("Property type is undefined for json schema property '" + property.type() + "' for keyword '" + methodName + "'");
			}
			propertyParamsBuilder.add("type", property.type());

			if (property.defaultV() != null && !property.defaultV().isEmpty()) {
				propertyParamsBuilder.add("default", property.defaultV());
			}
			
			if (property.nestedPropertiesRefs() != null && property.nestedPropertiesRefs().length > 0) {
				if (!Objects.equals("object", property.type())) {
					throw new JsonSchemaPreparationException("Property having nested properties must have \"object\" type");
				}
				includeNestedProperties(propertyBuilder, Arrays.asList(property.nestedPropertiesRefs()), propertyRefsRegistry, methodName);
			}

			propertiesBuilder.add(propertyBuilder);

			if (property.required()) {
				requiredProperties.add(property.name());
			}

		}
		JsonArrayBuilder requiredBuilder = Json.createArrayBuilder();
		for (String requiredProperty : requiredProperties) {
			requiredBuilder.add(requiredProperty);
		}
		topLevelBuilder.add("required", requiredBuilder);

		return topLevelBuilder.build();
	}

	private void includeNestedProperties(JsonObjectBuilder propertyBuilder, List<String> nestedPropertiesRefs, Map<String, JsonSchemaProperty> propertyRefsRegistry, String methodName) throws JsonSchemaPreparationException {
		for (String nestedPropertiesRef : nestedPropertiesRefs) {
			JsonSchemaProperty nestedProperty = getPropertyByRef(nestedPropertiesRef, propertyRefsRegistry, methodName);

		}
	}

	private JsonSchemaProperty getPropertyByRef(String ref, Map<String, JsonSchemaProperty> propertyRefsRegistry, String methodName) throws JsonSchemaPreparationException {
		if (ref == null || ref.isEmpty()) {
			throw new JsonSchemaPreparationException("Unable to resolve json schema property ref '  " + ref + " ' for keyword '" + methodName + "'");
		}

		JsonSchemaProperty property = null;
		while (ref != null && !ref.isEmpty()) {
			property = propertyRefsRegistry.get(ref);
			if (property == null) {
				throw new JsonSchemaPreparationException("Unable to resolve json schema property ref '  " + ref + " ' for keyword '" + methodName + "'");
			}
			ref = property.propertyRef();
		}
		return property;
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
