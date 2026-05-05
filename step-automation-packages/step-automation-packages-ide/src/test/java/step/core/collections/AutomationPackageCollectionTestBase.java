/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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
package step.core.collections;

import ch.exense.commons.app.Configuration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import step.artefacts.Echo;
import step.artefacts.Sequence;
import step.automation.packages.AutomationPackageHookRegistry;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.JavaAutomationPackageReader;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.yaml.deserialization.AutomationPackageConcurrentEditException;
import step.core.yaml.deserialization.AutomationPackagePerObjectSaveUnsupportedException;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.parameter.automation.AutomationPackageParametersRegistration;
import step.plans.parser.yaml.YamlPlan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class AutomationPackageCollectionTestBase {

    private final JavaAutomationPackageReader reader;

    protected final File sourceDirectory = new File("src/test/resources/samples/step-automation-packages-sample1");
    protected File destinationDirectory;
    protected final Path expectedFilesPath = sourceDirectory.toPath().resolve("expected");
    protected AutomationPackageYamlFragmentManager fragmentManager;

    public AutomationPackageCollectionTestBase() {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();

        // accessor is not required in this test - we only read the yaml and don't store the result anywhere
        AutomationPackageParametersRegistration.registerParametersHooks(hookRegistry, serializationRegistry, Mockito.mock(ParameterManager.class));

        this.reader = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serializationRegistry, new Configuration());
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        destinationDirectory = Files.createTempDirectory("automationPackageCollectionTest").toFile();
        FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

        fragmentManager = reader.getAutomationPackageYamlFragmentManager(destinationDirectory);
    }

    @After
    public void tearDown() throws IOException, AutomationPackageReadingException {
        // Attempt to re-read the just written Automation package from scratch
        reader.getAutomationPackageYamlFragmentManager(destinationDirectory);
        FileUtils.deleteDirectory(destinationDirectory);
    }


    protected void assertFilesEqual(Path expected, Path actual) throws IOException {
        String expectedLines = Files.readString(expected);
        String actualLines = Files.readString(actual);

        assertEquals(expectedLines, actualLines);
    }

    protected void setPropertiesWriteToFragment(String entityName, String fragment) {
        Properties properties = new Properties();
        properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_PATH, entityName), fragment);
        properties.setProperty(String.format(AutomationPackageYamlFragmentManager.PROPERTY_NEW_OBJECT_FRAGMENT_MODE, entityName), AutomationPackageYamlFragmentManager.NewObjectFragmentMode.FRAGMENT.name());

        fragmentManager.setProperties(properties);
    }
}
