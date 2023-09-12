package step.automation.packages.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.AutomationPackageFile;
import step.automation.packages.AutomationPackageReadingException;
import step.core.accessors.DefaultJacksonMapperProvider;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AutomationPackageFileManagerTest {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageFileManagerTest.class);

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
        URL resource = validPackage.getResource("jmeterProject1/jmeterProject1.xml");
        log.info("Resource url: {}", resource.toString());
        Assert.assertNotNull(resource);

        try(InputStream is = validPackage.getResourceAsStream("jmeterProject1/jmeterProject1.xml")){
            Assert.assertNotNull(is);
            log.info("Resource: {}", new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    protected ObjectMapper createYamlObjectMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();

        // Disable native type id to enable conversion to generic Documents
        yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);

        return DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);
    }
}