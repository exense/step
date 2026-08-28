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
package step.engine.plugins;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The cache directory of a local execution is a temporary directory nothing else deletes: the JVM does not
 * remove what {@code Files.createTempDirectory} creates, so leaving it to the operating system means one
 * abandoned tree per run, holding whatever the run materialised.
 */
public class LocalApResourceCacheRootTest {

    @Test
    public void createsTheDirectoryAndDeletesItOnClose() throws IOException {
        File root;
        try (LocalApResourceCacheRoot cacheRoot = LocalApResourceCacheRoot.create()) {
            root = cacheRoot.getRoot();

            assertTrue(root.isDirectory());
            assertTrue(root.getName(), root.getName().startsWith(LocalApResourceCacheRoot.DIRECTORY_PREFIX));
            // the materialised entries of a package, in the shape ApResourceMaterializer writes them
            Files.createDirectories(root.toPath().resolve("6a8c19d348f0d75ed5b45f77/keywords"));
            Files.writeString(root.toPath().resolve("6a8c19d348f0d75ed5b45f77/keywords/kw.groovy"), "materialised");
        }

        assertFalse(root.getAbsolutePath(), root.exists());
    }

    /**
     * The cache is closed as an attribute of the execution engine context, and a caller holding it may
     * close it too: the second call must find nothing to do rather than fail.
     */
    @Test
    public void closingTwiceIsANoOp() {
        LocalApResourceCacheRoot cacheRoot = LocalApResourceCacheRoot.create();
        File root = cacheRoot.getRoot();

        cacheRoot.close();
        cacheRoot.close();

        assertFalse(root.exists());
    }

    @Test
    public void deletesADirectoryThatWasAlreadyRemovedElsewhere() throws IOException {
        LocalApResourceCacheRoot cacheRoot = LocalApResourceCacheRoot.create();
        Path root = cacheRoot.getRoot().toPath();
        Files.delete(root);

        cacheRoot.close();

        assertFalse(Files.exists(root));
    }

    /**
     * Two engines in one JVM - the CLI running twice, a suite next to another - must not share a cache,
     * or closing the first would empty the second.
     */
    @Test
    public void givesEachInstanceItsOwnDirectory() {
        try (LocalApResourceCacheRoot first = LocalApResourceCacheRoot.create();
             LocalApResourceCacheRoot second = LocalApResourceCacheRoot.create()) {
            assertFalse(first.getRoot().getAbsolutePath().equals(second.getRoot().getAbsolutePath()));
            assertEquals(first.getRoot().getParentFile(), second.getRoot().getParentFile());
        }
    }
}
