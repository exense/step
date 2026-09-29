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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import step.automation.packages.AutomationPackageReadingException;
import step.resources.LocalResourceManagerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The upgrade of a package writes back the files it migrated. The entities the editor does not support, here the
 * schedules and the alerting rules for which no model is registered, must come out of it unchanged, whether they sit in
 * the descriptor, in a fragment written because it holds a supported entity, or in a fragment written because it
 * declares its own version
 */
public class AutomationPackageUpgradeUnsupportedEntitiesTest extends AutomationPackageCollectionTestBase {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final List<String> UNSUPPORTED_FIELDS = List.of("schedules", "alertingRules");
    private static final List<String> FILES = List.of("automation-package.yml", "schedules.yml", "plans/Versioned.yml", "plans/Inherited.yml");

    public AutomationPackageUpgradeUnsupportedEntitiesTest() {
        super(new File("src/test/resources/testdata/ap-upgrade-unsupported-entities"));
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
    public void testUnsupportedEntitiesAreKeptByTheUpgrade() throws IOException, AutomationPackageReadingException {
        reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, true);

        for (String file : FILES) {
            JsonNode original = YAML.readTree(sourceDirectory.toPath().resolve(file).toFile());
            String upgradedYaml = Files.readString(destinationDirectory.toPath().resolve(Path.of(file)));
            JsonNode upgraded = YAML.readTree(upgradedYaml);
            assertNotNull(file + " is empty", upgraded);
            for (String field : UNSUPPORTED_FIELDS) {
                assertEquals(file + ":\n" + upgradedYaml, original.get(field), upgraded.get(field));
            }
        }

        // The package is now a current one, it can be read without upgrade
        reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, false);
    }
}
