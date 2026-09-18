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
package step.automation.packages;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class ApResourceCacheTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void apDirectoryUsesApIdUnderRoot() {
        File root = new File("AP_cache");
        assertEquals(new File(root, "apA"), ApResourceCache.apDirectory(root, "apA"));
    }

    @Test
    public void wipeRemovesTheApDirectoryOnly() throws Exception {
        File root = tmp.newFolder("AP_cache");
        File apA = ApResourceCache.apDirectory(root, "apA");
        File apB = ApResourceCache.apDirectory(root, "apB");
        writeFile(new File(apA, "scripts/kw.groovy"), "x");
        writeFile(new File(apB, "data.csv"), "y");

        assertTrue(ApResourceCache.wipe(root, "apA"));

        assertFalse("apA dir must be gone", apA.exists());
        assertTrue("sibling apB must be untouched", new File(apB, "data.csv").exists());
    }

    @Test
    public void wipeIsNoOpWhenDirectoryMissing() throws Exception {
        File root = tmp.newFolder("AP_cache");
        assertTrue(ApResourceCache.wipe(root, "never-materialised"));
    }

    @Test
    public void wipeIsNoOpOnNullArguments() {
        assertTrue(ApResourceCache.wipe(null, "apA"));
        assertTrue(ApResourceCache.wipe(new File("AP_cache"), null));
    }

    private static void writeFile(File file, String content) throws Exception {
        Files.createDirectories(file.getParentFile().toPath());
        Files.writeString(file.toPath(), content);
    }
}
