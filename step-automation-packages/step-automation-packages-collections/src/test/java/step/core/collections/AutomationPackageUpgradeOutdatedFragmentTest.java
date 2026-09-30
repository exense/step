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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.LegacyAutomationPackageSchemaVersionSetException;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.resources.LocalResourceManagerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A package whose descriptor is current may still import a fragment declaring an older version of its own: the package
 * has to be upgraded as well, and the upgrade only rewrites that fragment
 */
public class AutomationPackageUpgradeOutdatedFragmentTest extends AutomationPackageCollectionTestBase {

    private static final String CURRENT_VERSION = YamlAutomationPackageVersions.ACTUAL_VERSION.toString();

    public AutomationPackageUpgradeOutdatedFragmentTest() {
        super(new File("src/test/resources/testdata/ap-upgrade-outdated-fragment"));
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
    public void testOutdatedFragmentRequiresUpgrade() {
        LegacyAutomationPackageSchemaVersionSetException e = Assert.assertThrows(LegacyAutomationPackageSchemaVersionSetException.class,
            () -> reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, false));
        // The message tells which fragment is outdated
        assertTrue(e.getMessage(), e.getMessage().contains("plans/Outdated.yml"));
        assertTrue(e.getMessage(), e.getMessage().contains("1.0.0"));
    }

    @Test
    public void testOnlyTheOutdatedFragmentIsUpgraded() throws IOException, AutomationPackageReadingException {
        reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, true);

        String outdated = read("plans/Outdated.yml");
        assertTrue(outdated, outdated.contains(CURRENT_VERSION));
        assertFalse(outdated, outdated.contains("1.0.0"));
        assertTrue(outdated, outdated.contains("http://$${host}"));
        assertTrue(outdated, outdated.contains("plan: \"Current\""));
        assertFalse(outdated, outdated.contains("selectionAttributes"));

        // The files already current are left untouched
        assertFilesEqual(sourceDirectory.toPath().resolve("automation-package.yml"), destinationDirectory.toPath().resolve("automation-package.yml"));
        assertFilesEqual(sourceDirectory.toPath().resolve("plans").resolve("Current.yml"), destinationDirectory.toPath().resolve("plans").resolve("Current.yml"));

        // The package is now current
        reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, false);
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(destinationDirectory.toPath().resolve(Path.of(relativePath)));
    }
}
