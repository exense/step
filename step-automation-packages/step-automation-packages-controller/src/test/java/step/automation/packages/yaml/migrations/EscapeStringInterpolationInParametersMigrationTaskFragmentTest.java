/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.automation.packages.yaml.migrations;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import ch.exense.commons.app.Configuration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import step.automation.packages.AutomationPackageContent;
import step.automation.packages.AutomationPackageFromFolderProvider;
import step.automation.packages.AutomationPackageHookRegistry;
import step.automation.packages.JavaAutomationPackageArchive;
import step.automation.packages.JavaAutomationPackageReader;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;
import step.parameter.ParameterManager;
import step.parameter.automation.AutomationPackageParameter;
import step.parameter.automation.AutomationPackageParameterJsonSchema;
import step.parameter.automation.AutomationPackageParametersRegistration;

/**
 * An automation package can declare its parameters in an imported fragment rather than in the descriptor itself.
 * Fragments carry no version of their own, so they have to inherit the one of the package importing them, otherwise
 * their values would silently escape the migration.
 */
public class EscapeStringInterpolationInParametersMigrationTaskFragmentTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final JavaAutomationPackageReader reader;

    public EscapeStringInterpolationInParametersMigrationTaskFragmentTest() {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();
        AutomationPackageScheduleRegistration.registerSerialization(serializationRegistry);
        AutomationPackageParametersRegistration.registerParametersHooks(hookRegistry, serializationRegistry, Mockito.mock(ParameterManager.class));
        this.reader = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH,
            hookRegistry, serializationRegistry, new Configuration());
    }

    @Test
    public void testParametersOfAFragmentAreMigratedWithThePackage() throws Exception {
        List<AutomationPackageParameter> parameters = readParameters("1.2.0");
        Assert.assertEquals("http://$${host}:8080", parameters.get(0).getValue().getValue());
    }

    @Test
    public void testParametersOfAFragmentAreNotMigratedForACurrentPackage() throws Exception {
        List<AutomationPackageParameter> parameters = readParameters(YamlAutomationPackageVersions.ACTUAL_VERSION.toString());
        Assert.assertEquals("http://${host}:8080", parameters.get(0).getValue().getValue());
    }

    @SuppressWarnings("unchecked")
    private List<AutomationPackageParameter> readParameters(String packageVersion) throws Exception {
        File apFolder = folder.newFolder("package");
        // The parameters live in an imported fragment, which declares no version of its own
        Files.writeString(Path.of(apFolder.getPath(), "automation-package.yml"),
            "version: " + packageVersion + "\n" +
                "name: \"fragment-package\"\n" +
                "fragments:\n" +
                "  - \"parameters.yml\"\n", StandardCharsets.UTF_8);
        Files.writeString(Path.of(apFolder.getPath(), "parameters.yml"),
            "parameters:\n" +
                "  - key: \"url\"\n" +
                "    value: \"http://${host}:8080\"\n", StandardCharsets.UTF_8);

        try (AutomationPackageFromFolderProvider provider = new AutomationPackageFromFolderProvider(apFolder, null)) {
            AutomationPackageContent content = reader.readAutomationPackage(
                (JavaAutomationPackageArchive) provider.getAutomationPackageArchive(), null, false);
            List<AutomationPackageParameter> parameters =
                (List<AutomationPackageParameter>) content.getAdditionalData(AutomationPackageParameterJsonSchema.FIELD_NAME_IN_AP);
            Assert.assertEquals(1, parameters.size());
            return parameters;
        }
    }
}
