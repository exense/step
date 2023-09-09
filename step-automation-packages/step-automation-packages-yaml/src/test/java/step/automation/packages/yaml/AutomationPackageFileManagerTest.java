package step.automation.packages.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.junit.Assert;
import org.junit.Test;
import step.automation.packages.AutomationPackageFile;
import step.automation.packages.AutomationPackageReadingException;
import step.core.accessors.DefaultJacksonMapperProvider;

import java.io.File;
import java.io.IOException;

public class AutomationPackageFileManagerTest {

    private ObjectMapper yamlObjectMapper;

    public AutomationPackageFileManagerTest() {
        this.yamlObjectMapper = createYamlObjectMapper();
    }

    @Test
    public void isAutomationPackage() throws AutomationPackageReadingException, IOException {
        File automationPackageJar = new File("src/test/resources/step/functions/packages/yaml/testpack.jar");
        File malformedPackageJar = new File("src/test/resources/step/functions/packages/yaml/malformedPackage.jar");

        AutomationPackageFile validPackage = new AutomationPackageFile(automationPackageJar);
        Assert.assertTrue(validPackage.isAutomationPackage());

        JsonNode actualDescriptor = yamlObjectMapper.readTree(validPackage.getDescriptorYaml());
        JsonNode expectedDescriptor = yamlObjectMapper.readTree(new File("src/test/resources/step/functions/packages/yaml/expectedTestpackDescriptor.yml"));
        Assert.assertEquals(expectedDescriptor, actualDescriptor);

        Assert.assertFalse(new AutomationPackageFile(malformedPackageJar).isAutomationPackage());
    }

    protected ObjectMapper createYamlObjectMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();

        // Disable native type id to enable conversion to generic Documents
        yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);

        return DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);
    }
}