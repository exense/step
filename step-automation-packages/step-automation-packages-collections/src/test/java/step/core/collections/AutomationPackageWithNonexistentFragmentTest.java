package step.core.collections;

import ch.exense.commons.app.Configuration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import step.automation.packages.AutomationPackageHookRegistry;
import step.automation.packages.JavaAutomationPackageReader;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.parameter.ParameterManager;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.resources.LocalResourceManagerImpl;

import java.io.File;

public class AutomationPackageWithNonexistentFragmentTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testLoading() throws Exception {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();
        AutomationPackageParametersRegistration.registerParametersHooks(hookRegistry, serializationRegistry, Mockito.mock(ParameterManager.class));
        var reader = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serializationRegistry, new Configuration());
        try {
            reader.getAutomationPackageYamlFragmentManager(new File("src/test/resources/testdata/ap-with-nonexisting-fragment"), new LocalResourceManagerImpl(tempFolder.getRoot()));
            Assert.fail("Expected exception");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Illegal resource definition, resource cannot be found: nonexisting.yml", e.getMessage());
        }
    }
}
