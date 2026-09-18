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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Creation of files inside the automation package open in the editor. The files are the user's sources,
 * so the two properties worth testing are that nothing existing is ever overwritten and that a name
 * derived from an entity name is usable as a file name.
 */
public class LocalApResourceWriterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path apRoot;

    @Before
    public void setUp() throws IOException {
        apRoot = tmp.newFolder("my-ap").toPath();
    }

    private static InputStream content(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void createsTheFileInTheGivenDirectory() throws IOException {
        String relativePath = LocalApResourceWriter.createFile(apRoot, "keywords", "My Keyword", "groovy",
            content("println 'hello'"));

        assertEquals("keywords/My_Keyword.groovy", relativePath);
        assertEquals("println 'hello'", Files.readString(apRoot.resolve(relativePath)));
    }

    @Test
    public void createsTheFileAtTheRootWhenNoDirectoryIsGiven() throws IOException {
        assertEquals("Kw.groovy", LocalApResourceWriter.createFile(apRoot, null, "Kw", "groovy", null));
        assertEquals("Kw2.js", LocalApResourceWriter.createFile(apRoot, "  ", "Kw2", "js", null));
    }

    @Test
    public void createsAnEmptyFileWithoutContent() throws IOException {
        String relativePath = LocalApResourceWriter.createFile(apRoot, "keywords", "Kw", "groovy", null);

        assertTrue(Files.exists(apRoot.resolve(relativePath)));
        assertEquals("", Files.readString(apRoot.resolve(relativePath)));
    }

    @Test
    public void neverOverwritesAnExistingFile() throws IOException {
        Files.createDirectories(apRoot.resolve("keywords"));
        Files.writeString(apRoot.resolve("keywords/Kw.groovy"), "the user's script");

        assertEquals("keywords/Kw_2.groovy", LocalApResourceWriter.createFile(apRoot, "keywords", "Kw", "groovy", null));
        assertEquals("keywords/Kw_3.groovy", LocalApResourceWriter.createFile(apRoot, "keywords", "Kw", "groovy", null));
        assertEquals("the user's script", Files.readString(apRoot.resolve("keywords/Kw.groovy")));
    }

    @Test
    public void refusesADirectoryEscapingThePackage() {
        assertThrows(IllegalArgumentException.class,
            () -> LocalApResourceWriter.createFile(apRoot, "../elsewhere", "Kw", "groovy", null));
    }

    @Test
    public void createsAMissingFileAndLeavesAnExistingOneAlone() throws IOException {
        File created = LocalApResourceWriter.createFileIfMissing(apRoot, "scripts/kw.groovy", content("template"));
        assertEquals("template", Files.readString(created.toPath()));

        File existing = LocalApResourceWriter.createFileIfMissing(apRoot, "scripts/kw.groovy", content("template"));
        assertEquals(created.getAbsolutePath(), existing.getAbsolutePath());
        assertEquals("template", Files.readString(existing.toPath()));
    }

    /**
     * The name is derived by {@link ApFileNames}, tested there; what matters here is that a name it
     * refuses is not turned into a file by some fallback of ours.
     */
    @Test
    public void namesTheFileAfterTheEntityAndRefusesWhatItCannotName() throws IOException {
        assertEquals("keywords/My_Keyword.groovy",
            LocalApResourceWriter.createFile(apRoot, "keywords", "  My   Keyword  ", "groovy", null));
        assertThrows(IllegalArgumentException.class,
            () -> LocalApResourceWriter.createFile(apRoot, "keywords", "...", "groovy", null));
    }
}
