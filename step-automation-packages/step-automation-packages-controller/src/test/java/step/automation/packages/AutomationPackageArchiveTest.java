package step.automation.packages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.DefaultJacksonMapperProvider;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AutomationPackageArchiveTest {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageArchiveTest.class);

    private final ObjectMapper yamlObjectMapper;

    public AutomationPackageArchiveTest() {
        this.yamlObjectMapper = createYamlObjectMapper();
    }

    @Test
    public void isAutomationPackage() throws AutomationPackageReadingException, IOException {
        File automationPackageJar = new File("src/test/resources/samples/step-automation-packages-sample2.jar");

        try (AutomationPackageArchive validPackage = new JavaAutomationPackageArchive(automationPackageJar, null)) {
            Assert.assertTrue(validPackage.hasAutomationPackageDescriptor());

            try (InputStream yaml = validPackage.getDescriptorYaml()) {
                JsonNode actualDescriptor = yamlObjectMapper.readTree(yaml);
                JsonNode expectedDescriptor = yamlObjectMapper.readTree(new File("src/test/resources/step/automation/packages/expectedTestpackDescriptor.yml"));
                Assert.assertEquals(expectedDescriptor, actualDescriptor);
            }

            URL resource = validPackage.getResource("jmeterProject1/jmeterProject1.xml");
            log.info("Resource url: {}", resource.toString());
            Assert.assertNotNull(resource);

            try (InputStream is = validPackage.getResourceAsStream("jmeterProject1/jmeterProject1.xml")) {
                Assert.assertNotNull(is);
                log.info("Resource: {}", new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        }

        File malformedPackageJar = new File("src/test/resources/samples/step-automation-packages-malformed.jar");
        try (AutomationPackageArchive malformed = new JavaAutomationPackageArchive(malformedPackageJar, null)) {
            boolean isAutomationPackage = malformed.hasAutomationPackageDescriptor();
            if (isAutomationPackage) {
                try (InputStream yaml = malformed.getDescriptorYaml()) {
                    JsonNode malformedDescriptor = yamlObjectMapper.readTree(yaml);
                    if (malformedDescriptor != null) {
                        log.error("Unexpected descriptor is found in malformed package: {}", malformedDescriptor);
                    } else {
                        log.error("Package without descriptor cannot be used as automation package");
                    }
                }
            }
            Assert.assertFalse(isAutomationPackage);
        }
    }

    protected ObjectMapper createYamlObjectMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();

        // Disable native type id to enable conversion to generic Documents
        yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);

        return DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);
    }
}