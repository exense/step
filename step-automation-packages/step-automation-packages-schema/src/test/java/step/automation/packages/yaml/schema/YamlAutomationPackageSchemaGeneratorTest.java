package step.automation.packages.yaml.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static step.automation.packages.yaml.YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH;

public class YamlAutomationPackageSchemaGeneratorTest {
    private static final Logger log = LoggerFactory.getLogger(YamlAutomationPackageSchemaGeneratorTest.class);

    private final ObjectMapper jsonObjectMapper = new ObjectMapper();

    @Test
    public void generateJsonSchema() throws IOException, JsonSchemaPreparationException {
        log.info("Generating actual json schema for simplified plan format");

        // read published json schema
        InputStream jsonSchemaFile = this.getClass().getClassLoader().getResourceAsStream(ACTUAL_JSON_SCHEMA_PATH);

        JsonNode publishedSchema = jsonObjectMapper.readTree(jsonSchemaFile);
        YamlAutomationPackageSchemaGenerator schemaGenerator = new YamlAutomationPackageSchemaGenerator("step", YamlAutomationPackageVersions.ACTUAL_VERSION);
        JsonNode currentSchema = schemaGenerator.generateJsonSchema();

        log.info("GENERATED SCHEMA:");
        log.info(currentSchema.toPrettyString());

        Files.writeString(Path.of("/Users/cyril/exense/step-backend/step/step-automation-packages/step-automation-packages-yaml/src/main/resources/step/automation/packages/yaml/step-automation-package-schema-os-1.3.0.json"), currentSchema.toPrettyString());
        String errorMessage = "Published schema doesn't match to the actual one. To fix the test you need to publish " +
            "the generated schema printed above and actualize the published schema in current test";
        Assert.assertEquals(errorMessage, publishedSchema.toPrettyString(), currentSchema.toPrettyString());
    }
}
