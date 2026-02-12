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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.Echo;
import step.automation.packages.AutomationPackageHookRegistry;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.JavaAutomationPackageReader;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.parameter.ParameterManager;
import step.parameter.automation.AutomationPackageParametersRegistration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class AutomationPackageCollectionTest {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageCollectionTest.class);

    private final JavaAutomationPackageReader reader;

    private final File sourceDirectory = new File("src/test/resources/samples/step-automation-packages-sample1");;
    private File destinationDirectory;
    private Collection<Plan> planCollection;
    private final Path expectedFilesPath = sourceDirectory.toPath().resolve("expected");

    public AutomationPackageCollectionTest() throws AutomationPackageReadingException {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();

        // accessor is not required in this test - we only read the yaml and don't store the result anywhere
        AutomationPackageParametersRegistration.registerParametersHooks(hookRegistry, serializationRegistry, Mockito.mock(ParameterManager.class));

        this.reader = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, serializationRegistry, new Configuration());
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        Properties properties = new Properties();
        destinationDirectory = Files.createTempDirectory("automationPackageCollectionTest").toFile();
        FileUtils.copyDirectory(sourceDirectory, destinationDirectory);

        AutomationPackageYamlFragmentManager fragmentManager = reader.provideAutomationPackageYamlFragmentManager(destinationDirectory);
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(properties, fragmentManager);
        planCollection = collectionFactory.getCollection("plan", Plan.class);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(destinationDirectory);
    }

    @Test
    public void testReadAllPlans()  {
        long count = planCollection.count(Filters.empty(), 100);
        List<Plan> plans = planCollection.find(Filters.empty(), null, null, null, 100).collect(Collectors.toList());

        assertEquals(2, count);
        Set<String> names = plans.stream().map(p -> p.getAttributes().get("name")).collect(Collectors.toUnmodifiableSet());

        assertEquals(2, names.size());

        assertTrue(names.contains("Test Plan"));
        assertTrue(names.contains("Test Plan with Composite"));
    }

    @Test
    public void testPlanModify() throws IOException {
        Optional<Plan> optionalPlan = planCollection.find(Filters.equals("attributes.name", "Test Plan"), null, null, null, 100).findFirst();

        assertTrue(optionalPlan.isPresent());

        Plan plan = optionalPlan.get();

        Echo firstEcho = (Echo) plan.getRoot().getChildren().get(0);
        DynamicValue<Object> text = firstEcho.getText();
        text.setDynamic(true);
        text.setExpression("new Date().toString();");

        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("plan1AfterModification.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));
    }


    @Test
    public void testPlanRenameExisting() throws IOException {
        Optional<Plan> optionalPlan = planCollection.find(Filters.equals("attributes.name", "Test Plan"), null, null, null, 100).findFirst();

        assertTrue(optionalPlan.isPresent());

        Plan plan = optionalPlan.get();

        plan.getAttributes().put("name", "New Plan Name");

        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("plan1AfterRename.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));
    }


    @Test
    public void testPlanRemoveExisting() throws IOException {
        planCollection.remove(Filters.equals("attributes.name", "Test Plan"));

        assertFilesEqual(expectedFilesPath.resolve("plan1AfterRemove.yml"), destinationDirectory.toPath().resolve("plans").resolve("plan1.yml"));
    }

    private void assertFilesEqual(Path expected, Path actual) throws IOException {
        List<String> expectedLines = Files.readAllLines(expected);
        List<String> actualLines = Files.readAllLines(actual);

        assertEquals(expectedLines, actualLines);
    }
}
