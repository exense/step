package step.core.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.plans.serialization.SimplifiedPlanJsonSchemaGenerator;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;

import java.io.File;
import java.io.IOException;

public class SimplifiedPlanJsonSchemaGeneratorTest {

	private static final Logger log = LoggerFactory.getLogger(SimplifiedPlanJsonSchemaGeneratorTest.class);

	private final ObjectMapper jsonObjectMapper = new ObjectMapper();

	@Test
	public void generateJsonSchema() throws IOException, JsonSchemaPreparationException {
		// read published json schema
		// TODO: how to publish schema and how to check it in test?
		File jsonSchemaFile = new File("src/test/resources/step/core/plans/serialization/simplified-plan-schema-published.json");

		JsonNode publishedSchema = jsonObjectMapper.readTree(jsonSchemaFile);
		SimplifiedPlanJsonSchemaGenerator schemaGenerator = new SimplifiedPlanJsonSchemaGenerator("step");
		JsonNode currentSchema = schemaGenerator.generateJsonSchema();

		log.info("GENERATED SCHEMA:");
		log.info(currentSchema.toPrettyString());

		String errorMessage = "Published schema doesn't match to the actual one. To fix the test you need to publish " +
				"the generated schema printed above and actualize the published schema in current test";
		Assert.assertEquals(errorMessage, publishedSchema, currentSchema);
	}
}