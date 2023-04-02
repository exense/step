package step.functions.packages.handlers;

import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;
import step.handlers.javahandler.Input;
import step.handlers.javahandler.Keyword;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class KeywordJsonSchemaReader {

	public JsonObject readJsonSchemaForKeyword(Method method) throws JsonSchemaPreparationException{
		Keyword keywordAnnotation = method.getAnnotation(Keyword.class);

		boolean useTextJsonSchema = keywordAnnotation.schema() != null && !keywordAnnotation.schema().isEmpty();
		boolean useAnnotatedJsonInputs = method.getParameters() != null && Arrays.stream(method.getParameters()).anyMatch(p -> p.isAnnotationPresent(Input.class));
		if (useTextJsonSchema && useAnnotatedJsonInputs) {
			throw new IllegalArgumentException("Ambiguous definition of json schema for keyword. You should define either 'jsonSchema' or 'schema' parameter in @Keyword annotation");
		}

		if (useTextJsonSchema) {
			return readJsonSchemaFromPlainText(keywordAnnotation.schema(), method);
		} else if (useAnnotatedJsonInputs) {
			return readJsonSchemaFromInputAnnotations(method);
		} else {
			return createEmptyJsonSchema();
		}
	}

	private JsonObject readJsonSchemaFromInputAnnotations(Method method) throws JsonSchemaPreparationException {
		JsonObjectBuilder topLevelBuilder = Json.createObjectBuilder();
		// top-level type is always 'object'
		topLevelBuilder.add("type", "object");

		JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
		List<String> requiredProperties = new ArrayList<>();
		for (Parameter p : method.getParameters()) {
			JsonObjectBuilder propertyParamsBuilder = Json.createObjectBuilder();

			if (!p.isAnnotationPresent(Input.class)) {
				throw new JsonSchemaPreparationException("Parameter " + p.getName() + " is not annotated with " + Input.class.getName());
			}

			Input inputAnnotation = p.getAnnotation(Input.class);

			String parameterName = inputAnnotation.name();
			if(parameterName == null || parameterName.isEmpty()){
				throw new JsonSchemaPreparationException("Parameter name is not resolved for parameter " + p.getName());
			}

			String type = resolveJsonPropertyType(p);
			propertyParamsBuilder.add("type", type);

			if (inputAnnotation.defaultValue() != null && !inputAnnotation.defaultValue().isEmpty()) {
				addDefaultValue(p, inputAnnotation.defaultValue(), propertyParamsBuilder);
			}

			if (inputAnnotation.required()) {
				requiredProperties.add(parameterName);
			}

			propertiesBuilder.add(parameterName, propertyParamsBuilder);
		}
		topLevelBuilder.add("properties", propertiesBuilder);

		JsonArrayBuilder requiredBuilder = Json.createArrayBuilder();
		for (String requiredProperty : requiredProperties) {
			requiredBuilder.add(requiredProperty);
		}
		topLevelBuilder.add("required", requiredBuilder);
		return topLevelBuilder.build();
	}

	private void addDefaultValue(Parameter p, String defaultValue, JsonObjectBuilder builder) throws JsonSchemaPreparationException {
		if(String.class.isAssignableFrom(p.getType())){
			builder.add("default", defaultValue);
		} else if(Boolean.class.isAssignableFrom(p.getType())){
			builder.add("default", Boolean.parseBoolean(defaultValue));
		} else if(Integer.class.isAssignableFrom(p.getType())){
			builder.add("default", Integer.parseInt(defaultValue));
		} else if(Long.class.isAssignableFrom(p.getType())){
			builder.add("default", Long.parseLong(defaultValue));
		} else if(Double.class.isAssignableFrom(p.getType())){
			builder.add("default", Double.parseDouble(defaultValue));
		} else if(BigInteger.class.isAssignableFrom(p.getType())){
			builder.add("default", BigInteger.valueOf(Long.parseLong(defaultValue)));
		} else if(BigDecimal.class.isAssignableFrom(p.getType())){
			builder.add("default", BigDecimal.valueOf(Double.parseDouble(defaultValue)));
		} else {
			throw new JsonSchemaPreparationException("Unable to resolve default value for parameter " + p.getName());
		}
	}

	private String resolveJsonPropertyType(Parameter p) {
		if (String.class.isAssignableFrom(p.getType())) {
			return "string";
		} else if (Boolean.class.isAssignableFrom(p.getType())) {
			return "boolean";
		} else if (Number.class.isAssignableFrom(p.getType())) {
			return "number";
		} else {
			return "object";
		}
	}

	protected JsonObject createEmptyJsonSchema() {
		return Json.createObjectBuilder().build();
	}

	protected JsonObject readJsonSchemaFromPlainText(String schema, Method method) throws JsonSchemaPreparationException {
		try {
			return Json.createReader(new StringReader(schema)).readObject();
		} catch (JsonParsingException e) {
			throw new JsonSchemaPreparationException("Parsing error in the schema for keyword '" + method.getName() + "'. The error was: " + e.getMessage());
		} catch (JsonException e) {
			throw new JsonSchemaPreparationException("I/O error in the schema for keyword '" + method.getName() + "'. The error was: " + e.getMessage());
		} catch (Exception e) {
			throw new JsonSchemaPreparationException("Unknown error in the schema for keyword '" + method.getName() + "'. The error was: " + e.getMessage());
		}
	}

}
