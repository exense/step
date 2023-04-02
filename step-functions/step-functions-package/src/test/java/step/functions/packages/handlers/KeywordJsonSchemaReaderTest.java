package step.functions.packages.handlers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Input;
import step.handlers.javahandler.Keyword;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class KeywordJsonSchemaReaderTest {

	public static final Logger log = LoggerFactory.getLogger(KeywordJsonSchemaReaderTest.class);

	@Test
	public void jsonInputParamsReaderTest() throws JsonSchemaPreparationException, IOException {
		KeywordJsonSchemaReader reader = new KeywordJsonSchemaReader();

		Method method = Arrays.stream(KeywordTestClass.class.getMethods()).filter(m -> m.getName().equals("MyKeywordWithInputAnnotation")).findFirst().orElseThrow();

		log.info("Check json schema for method " + method.getName());
		JsonObject schema = reader.readJsonSchemaForKeyword(method);
		String jsonString = schema.toString();
		log.info(jsonString);

		File expectedSchema = new File("src/test/resources/step/functions/packages/handlers/expected-json-schema-1.json");

		JsonFactory factory = new JsonFactory();
		ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper(factory);

		// compare json nodes to avoid unstable comparisons in case of changed whitespaces or fields ordering
		JsonNode expectedJsonNode = mapper.readTree(expectedSchema);
		JsonNode actualJsonNode = mapper.readTree(jsonString);

		Assert.assertEquals(expectedJsonNode, actualJsonNode);
	}

	public static class KeywordTestClass extends AbstractKeyword {
		@Keyword
		public void MyKeywordWithInputAnnotation(@Input(name = "numberField", defaultValue = "1", required = true) Integer numberField,
												 @Input(name = "booleanField", defaultValue = "true", required = true) Boolean booleanField,
												 @Input(name = "stringField", defaultValue = "myValue", required = true) String stringField,
												 @Input(name = "stringField2", defaultValue = "myValue2") String secondStringField) {
			output.add("test", "test");
		}
	}

}