package step.functions.packages.handlers;

import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;
import step.handlers.javahandler.Input;
import step.handlers.javahandler.Keyword;

import java.io.StringReader;
import java.lang.reflect.Field;
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

			String type = resolveJsonPropertyType(p.getType());
			propertyParamsBuilder.add("type", type);

			if (inputAnnotation.defaultValue() != null && !inputAnnotation.defaultValue().isEmpty()) {
				addDefaultValue(inputAnnotation.defaultValue(), propertyParamsBuilder, p.getType(), parameterName);
			}

			if (inputAnnotation.required()) {
				requiredProperties.add(parameterName);
			}

			if(Objects.equals("object", type)){
				processNestedFields(propertyParamsBuilder, p.getType());
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

	private void processNestedFields(JsonObjectBuilder propertyParamsBuilder, Class<?> clazz) throws JsonSchemaPreparationException {
		JsonObjectBuilder nestedPropertiesBuilder = Json.createObjectBuilder();
		List<String> requiredProperties = new ArrayList<>();

		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			JsonObjectBuilder nestedPropertyParamsBuilder = Json.createObjectBuilder();

			if (!field.isAnnotationPresent(Input.class)) {
				throw new JsonSchemaPreparationException("Field " + field.getName() + " of " + clazz.getName() + " is not annotated with " + Input.class.getName());
			}

			Input input = field.getAnnotation(Input.class);
			String parameterName = input.name() == null || input.name().isEmpty() ? field.getName() : input.name();

			if (input.required()) {
				requiredProperties.add(parameterName);
			}

			String type = resolveJsonPropertyType(field.getType());
			nestedPropertyParamsBuilder.add("type", type);

			if (input.defaultValue() != null && !input.defaultValue().isEmpty()) {
				addDefaultValue(input.defaultValue(), nestedPropertyParamsBuilder, field.getType(), parameterName);
			}

			if (Objects.equals("object", type)) {
				processNestedFields(nestedPropertyParamsBuilder, field.getType());
			}

			nestedPropertiesBuilder.add(parameterName, nestedPropertyParamsBuilder);

		}
		propertyParamsBuilder.add("properties", nestedPropertiesBuilder);

		JsonArrayBuilder requiredBuilder = Json.createArrayBuilder();
		for (String requiredProperty : requiredProperties) {
			requiredBuilder.add(requiredProperty);
		}
		propertyParamsBuilder.add("required", requiredBuilder);
	}

	private void addDefaultValue(String defaultValue, JsonObjectBuilder builder, Class<?> type, String paramName) throws JsonSchemaPreparationException {
		if(String.class.isAssignableFrom(type)){
			builder.add("default", defaultValue);
		} else if(Boolean.class.isAssignableFrom(type)){
			builder.add("default", Boolean.parseBoolean(defaultValue));
		} else if(Integer.class.isAssignableFrom(type)){
			builder.add("default", Integer.parseInt(defaultValue));
		} else if(Long.class.isAssignableFrom(type)){
			builder.add("default", Long.parseLong(defaultValue));
		} else if(Double.class.isAssignableFrom(type)){
			builder.add("default", Double.parseDouble(defaultValue));
		} else if(BigInteger.class.isAssignableFrom(type)){
			builder.add("default", BigInteger.valueOf(Long.parseLong(defaultValue)));
		} else if(BigDecimal.class.isAssignableFrom(type)){
			builder.add("default", BigDecimal.valueOf(Double.parseDouble(defaultValue)));
		} else {
			throw new JsonSchemaPreparationException("Unable to resolve default value for parameter " + paramName);
		}
	}

	private String resolveJsonPropertyType(Class<?> type) {
		if (String.class.isAssignableFrom(type)) {
			return "string";
		} else if (Boolean.class.isAssignableFrom(type)) {
			return "boolean";
		} else if (Number.class.isAssignableFrom(type)) {
			return "number";
		} else {
			return "object";
		}

		// TODO: support arrays?
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
