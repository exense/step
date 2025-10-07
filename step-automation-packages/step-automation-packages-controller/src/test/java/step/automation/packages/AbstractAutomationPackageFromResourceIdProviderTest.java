/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */

package step.automation.packages;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.kwlibrary.KeywordLibraryFromResourceIdProvider;
import step.resources.*;

import java.io.*;

public class AbstractAutomationPackageFromResourceIdProviderTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractAutomationPackageFromResourceIdProviderTest.class);

    private static final String SAMPLE1_FILE_NAME = "step-automation-packages-sample1.jar";
    private static final String KW_LIB_FILE_NAME = "step-automation-packages-kw-lib.jar";

    private LocalResourceManagerImpl resourceManager;

    @Before
    public void before() {
        this.resourceManager = new LocalResourceManagerImpl();

    }

    @Test
    public void testResourceCleanup() {
        File automationPackageJar = new File("src/test/resources/samples/" + SAMPLE1_FILE_NAME);
        File kwLibSnapshotJar = new File("src/test/resources/samples/" + KW_LIB_FILE_NAME);

        Resource savedApResource;
        Resource savedkwResource;
        try (InputStream is = new FileInputStream(automationPackageJar);
             InputStream kwIs = new FileInputStream(kwLibSnapshotJar);) {
            savedApResource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_AP, is, SAMPLE1_FILE_NAME, null, "testUser");
            savedkwResource = resourceManager.createResource(ResourceManager.RESOURCE_TYPE_AP_LIBRARY, kwIs, KW_LIB_FILE_NAME, null, "testUser");
        } catch (IOException | InvalidResourceFormatException e) {
            throw new RuntimeException("Unexpected exception", e);
        }

        AutomationPackageArchive archive;
        try (KeywordLibraryFromResourceIdProvider kwLibProvider = new KeywordLibraryFromResourceIdProvider(resourceManager, savedkwResource.getId().toHexString(), o -> true);
             AutomationPackageFromResourceIdProvider provider = new AutomationPackageFromResourceIdProvider(resourceManager, savedApResource.getId().toHexString(), kwLibProvider, o -> true)) {
            archive = provider.getAutomationPackageArchive();
        } catch (IOException | AutomationPackageReadingException e) {
            throw new RuntimeException("Unexpected exception", e);
        }

        Assert.assertNotNull(archive);
        ResourceRevisionFileHandle kwLibFile = resourceManager.getResourceFile(savedkwResource.getId().toHexString());
        ResourceRevisionFileHandle apFile = resourceManager.getResourceFile(savedApResource.getId().toHexString());

        // check that all used can be deleted (not blocked)
        log.info("Delete AP resource: {}", savedApResource.getId());
        resourceManager.deleteResource(savedApResource.getId().toHexString());

        log.info("Delete keyword resource: {}", savedkwResource.getId());
        resourceManager.deleteResource(savedkwResource.getId().toHexString());

        Assert.assertFalse(kwLibFile.getResourceFile().exists());
        Assert.assertFalse(apFile.getResourceFile().exists());
    }

    @After
    public void after() {
        if (resourceManager != null) {
            resourceManager.cleanup();
        }
    }

}