package step.core.collections;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.LegacyAutomationPackageSchemaVersionSetException;
import step.automation.packages.NoAutomationPackageSchemaVersionSetException;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.resources.LocalResourceManagerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AutomationPackageUpgradeTest extends AutomationPackageCollectionTestBase {

    public AutomationPackageUpgradeTest() {
        super(new File("src/test/resources/testdata/ap1-upgrade-legacy"));
    }

    @Before
    @Override
    public void setUp() throws IOException, AutomationPackageReadingException {
        destinationDirectory = Files.createTempDirectory("automationPackageCollectionTest").toFile();
        resourcesDirectory = Files.createTempDirectory("automationPackageCollectionTestResources").toFile();
        FileUtils.copyDirectory(sourceDirectory, destinationDirectory);
        resourceManager = new LocalResourceManagerImpl(resourcesDirectory);
    }


    @Test
    public void testLoadLegacyUpgradeFalse() {
        LegacyAutomationPackageSchemaVersionSetException e = Assert.assertThrows(LegacyAutomationPackageSchemaVersionSetException.class, () ->
            reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, false)
        );
        // The message tells which version the package declares and to which one it has to be upgraded
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("1.0.0"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains(YamlAutomationPackageVersions.ACTUAL_VERSION.toString()));
    }

    @Test
    public void testLoadLegacyUpgradeTrue() throws IOException {

        try {
            reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, true);

            assertFilesEqual(expectedFilesPath.resolve("descriptorAfterUpgrade.yml"), destinationDirectory.toPath().resolve("automation-package.yml"));
            assertFilesEqual(expectedFilesPath.resolve("keywordsAfterUpgrade.yml"), destinationDirectory.toPath().resolve("keywords.yml"));
            assertFilesEqual(sourceDirectory.toPath().resolve("parameters.yml"), destinationDirectory.toPath().resolve("parameters.yml"));
            assertFilesEqual(sourceDirectory.toPath().resolve("parameters2.yml"), destinationDirectory.toPath().resolve("parameters2.yml"));
            assertFilesEqual(sourceDirectory.toPath().resolve("schedules.yml"), destinationDirectory.toPath().resolve("schedules.yml"));
            assertFilesEqual(sourceDirectory.toPath().resolve("plans").resolve("plan1.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));
            assertFilesEqual(sourceDirectory.toPath().resolve("plans").resolve("plan2.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan2.yml"));

            //called once more with upgrade false as validation
            reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, false);
        } catch (AutomationPackageReadingException e) {
            Assert.fail("Caught " + e.getMessage());
        }
    }

    @Test
    public void testLoadNoVersionUpgradeFalse() {


        NoAutomationPackageSchemaVersionSetException e = Assert.assertThrows(NoAutomationPackageSchemaVersionSetException.class, () -> {
            Files.copy(destinationDirectory.toPath().resolve("automation-package-no-version.yml"), destinationDirectory.toPath().resolve("automation-package.yml"), StandardCopyOption.REPLACE_EXISTING);
            reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, false);
        });
        Assert.assertTrue(e.getMessage(), e.getMessage().contains(YamlAutomationPackageVersions.ACTUAL_VERSION.toString()));
    }

    /**
     * A descriptor declaring no version is read as a current one. The missing version must be reported before, even
     * when its content does not comply with the current schema
     */
    @Test
    public void testLoadNoVersionLegacyContentUpgradeFalse() throws IOException {
        Files.writeString(destinationDirectory.toPath().resolve("automation-package.yml"),
            "name: \"legacy content\"\n" +
            "plans:\n" +
            "  - name: \"legacy\"\n" +
            "    root:\n" +
            "      testCase:\n" +
            "        children:\n" +
            "          - beforeSequence:\n" +
            "              children:\n" +
            "                - echo:\n" +
            "                    text: \"before\"\n");

        Assert.assertThrows(NoAutomationPackageSchemaVersionSetException.class, () ->
            reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, false));

        // Upgrading does not migrate a package declaring no version, its legacy content is then rejected
        AutomationPackageReadingException e = Assert.assertThrows(AutomationPackageReadingException.class, () ->
            reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, true));
        Assert.assertFalse(e.getClass().getName(), e instanceof NoAutomationPackageSchemaVersionSetException);
    }

    @Test
    public void testLoadNoVersionUpgradeTrue() throws IOException {

        try {
            Files.copy(destinationDirectory.toPath().resolve("automation-package-no-version.yml"), destinationDirectory.toPath().resolve("automation-package.yml"), StandardCopyOption.REPLACE_EXISTING);

            reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, true);
            assertFilesEqual(expectedFilesPath.resolve("descriptorAfterUpgrade.yml"), destinationDirectory.toPath().resolve("automation-package.yml"));
            assertFilesEqual(sourceDirectory.toPath().resolve("keywords.yml"), destinationDirectory.toPath().resolve("keywords.yml"));
            assertFilesEqual(sourceDirectory.toPath().resolve("parameters.yml"), destinationDirectory.toPath().resolve("parameters.yml"));
            assertFilesEqual(sourceDirectory.toPath().resolve("parameters2.yml"), destinationDirectory.toPath().resolve("parameters2.yml"));
            assertFilesEqual(sourceDirectory.toPath().resolve("schedules.yml"), destinationDirectory.toPath().resolve("schedules.yml"));
            assertFilesEqual(sourceDirectory.toPath().resolve("plans").resolve("plan1.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));
            assertFilesEqual(sourceDirectory.toPath().resolve("plans").resolve("plan2.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan2.yml"));

            reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, false);


        } catch (AutomationPackageReadingException e) {
            Assert.fail("Caught " + e.getMessage());
        }
    }

    @After
    @Override
    public void tearDown() throws IOException, AutomationPackageReadingException {
        FileUtils.deleteDirectory(destinationDirectory);
        FileUtils.deleteDirectory(resourcesDirectory);
    }
}
