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
import static org.junit.Assert.assertTrue;

/**
 * The upgrade of a package writes back all the files it migrated. The entities the editor does not support are written
 * as they were migrated, whether they sit in the descriptor, in a fragment holding a supported entity, in a fragment
 * declaring its own version or in a fragment holding no supported entity at all.
 * <p>
 * The unsupported entities are here the schedules, for which no model is registered in this test, and the
 * unsupportedEntities, which stand for any field unknown to the reader, for instance a field of the enterprise edition.
 * No migration task concerns them, so they must come out of the upgrade unchanged
 */
public class AutomationPackageUpgradeUnsupportedEntitiesTest extends AutomationPackageCollectionTestBase {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final List<String> UNSUPPORTED_FIELDS = List.of("schedules", "unsupportedEntities");
    private static final List<String> FILES = List.of("automation-package.yml", "schedules.yml", "unsupported.yml", "plans/Versioned.yml", "plans/Inherited.yml");

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

        // The fragment holding no supported entity is written as well, from its migrated content: its values are
        // quoted by the serialization of the migrated document, unlike in the source
        String unsupported = Files.readString(destinationDirectory.toPath().resolve("unsupported.yml"));
        assertTrue(unsupported, unsupported.contains("name: \"fragmentEntity\""));

        // The package is now a current one, it can be read without upgrade
        reader.getAutomationPackageYamlFragmentManager(destinationDirectory, resourceManager, false);
    }
}
