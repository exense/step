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
package step.attachments;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class LocalApResourceProviderTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void resolvesAgainstLocalRoot() throws Exception {
        File root = tmp.newFolder("ap-folder");
        Files.createDirectories(new File(root, "data").toPath());
        Files.writeString(new File(root, "data/pool.csv").toPath(), "a,b");

        LocalApResourceProvider provider = new LocalApResourceProvider(root::toPath);
        File resolved = provider.resolve(FileResolver.LOCAL_AP_ID, "data/pool.csv");

        assertTrue(resolved.isFile());
        assertEquals("a,b", Files.readString(resolved.toPath()));
    }

    @Test(expected = ApResourceNotFoundException.class)
    public void missingFileThrowsNotFound() {
        File root = tmp.getRoot();
        LocalApResourceProvider provider = new LocalApResourceProvider(root::toPath);
        provider.resolve(FileResolver.LOCAL_AP_ID, "data/missing.csv");
    }

    @Test(expected = RuntimeException.class)
    public void missingRootThrows() {
        LocalApResourceProvider provider = new LocalApResourceProvider(() -> (Path) null);
        provider.resolve(FileResolver.LOCAL_AP_ID, "data/pool.csv");
    }

    @Test(expected = RuntimeException.class)
    public void rejectsTraversalEscape() {
        LocalApResourceProvider provider = new LocalApResourceProvider(tmp.getRoot()::toPath);
        provider.resolve(FileResolver.LOCAL_AP_ID, "../escape.txt");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonLocalApId() {
        LocalApResourceProvider provider = new LocalApResourceProvider(tmp.getRoot()::toPath);
        provider.resolve("64f0a1b2c3d4e5f6a7b8c9d0", "data/pool.csv");
    }

    @Test(expected = NullPointerException.class)
    public void rejectsNullRelativePath() {
        LocalApResourceProvider provider = new LocalApResourceProvider(tmp.getRoot()::toPath);
        provider.resolve(FileResolver.LOCAL_AP_ID, null);
    }
}
