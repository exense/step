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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.Echo;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.yaml.AutomationPackageYamlFragmentManager;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.plans.Plan;
import step.resources.LocalResourceManagerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A fragment may declare a schema version of its own. When the package is upgraded, such a fragment has to declare the
 * current version as well, otherwise it would be migrated a second time when read again
 */
public class AutomationPackageUpgradeFragmentVersionTest extends AutomationPackageCollectionTestBase {

    private static final String CURRENT_VERSION = YamlAutomationPackageVersions.ACTUAL_VERSION.toString();

    public AutomationPackageUpgradeFragmentVersionTest() {
        super(new File("src/test/resources/testdata/ap-upgrade-fragment-version"));
    }

    @Before
    @Override
    public void setUp() throws IOException {
        destinationDirectory = Files.createTempDirectory("automationPackageCollectionTest").toFile();
        resourcesDirectory = Files.createTempDirectory("automationPackageCollectionTestResources").toFile();
        FileUtils.copyDirectory(sourceDirectory, destinationDirectory);
        resourceManager = new LocalResourceManagerImpl(resourcesDirectory);
    }

    @After
    @Override
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(destinationDirectory);
        FileUtils.deleteDirectory(resourcesDirectory);
    }

    @Test
    public void testFragmentsAreUpgradedAndNotMigratedTwice() throws IOException, AutomationPackageReadingException {
        reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, true);

        String descriptor = read("automation-package.yml");
        assertTrue(descriptor, descriptor.contains("version: \"" + CURRENT_VERSION + "\"") || descriptor.contains("version: " + CURRENT_VERSION));

        // The fragment declaring its own version now declares the current one
        String versioned = read("plans/Versioned.yml");
        assertTrue(versioned, versioned.contains(CURRENT_VERSION));
        assertFalse(versioned, versioned.contains("1.2.0"));
        assertMigrated(versioned);

        // The fragment inheriting its version keeps inheriting it
        String inherited = read("plans/Inherited.yml");
        assertFalse(inherited, inherited.contains("version"));
        assertMigrated(inherited);

        // Read again as a current package: nothing is migrated anymore, the values are escaped exactly once
        AutomationPackageYamlFragmentManager fragmentManager = reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, false);
        for (String planName : new String[]{"Versioned", "Inherited"}) {
            Plan plan = StreamSupport.stream(fragmentManager.<Plan, Plan>getBusinessObjects(Plan.class).spliterator(), false)
                .filter(p -> planName.equals(p.getAttribute("name"))).findFirst().orElseThrow();
            assertEquals(planName, "http://$${host}", ((Echo) plan.getRoot().getChildren().get(0)).getText().getValue());
        }
    }

    private static void assertMigrated(String fragment) {
        assertTrue(fragment, fragment.contains("http://$${host}"));
        assertFalse(fragment, fragment.contains("selectionAttributes"));
        assertTrue(fragment, fragment.contains("plan: \"SubPlan\""));
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(destinationDirectory.toPath().resolve(Path.of(relativePath)));
    }
}
