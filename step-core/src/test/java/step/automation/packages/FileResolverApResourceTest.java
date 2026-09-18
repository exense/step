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

import org.junit.Test;
import step.attachments.FileResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Slice 1 coverage: {@code apResource:} prefix parsing, the {@code String.replace} trap,
 * {@code ..} traversal rejection and SPI routing in {@link FileResolver}.
 */
public class FileResolverApResourceTest {

    @Test
    public void isApResourceDistinguishesFromPlainResource() {
        assertTrue(FileResolver.isApResource("apResource:abc:scripts/kw.groovy"));
        assertFalse(FileResolver.isApResource("resource:64f0a1b2c3d4e5f6a7b8c9d0"));
        assertFalse(FileResolver.isApResource("plain/relative/path.csv"));
        assertFalse(FileResolver.isApResource(null));
        // A plain resource: reference must never be mistaken for an AP resource, and vice versa.
        assertFalse(FileResolver.isResource("apResource:abc:scripts/kw.groovy"));
    }

    @Test
    public void extractsApIdAndRelativePath() {
        String ref = FileResolver.createPathForApResource("64f0a1b2c3d4e5f6a7b8c9d0", "scripts/kw.groovy");
        assertEquals("apResource:64f0a1b2c3d4e5f6a7b8c9d0:scripts/kw.groovy", ref);
        assertEquals("64f0a1b2c3d4e5f6a7b8c9d0", FileResolver.extractApId(ref));
        assertEquals("scripts/kw.groovy", FileResolver.extractApRelativePath(ref));
    }

    @Test
    public void relativePathMayContainSeparator_splitOnFirstColonOnly() {
        // The relative path is split on the first ':' after the apId, so any ':' within the path
        // itself (e.g. an odd file name) is preserved verbatim.
        String ref = "apResource:abc:folder/weird:name.txt";
        assertEquals("abc", FileResolver.extractApId(ref));
        assertEquals("folder/weird:name.txt", FileResolver.extractApRelativePath(ref));
    }

    @Test
    public void doesNotFallIntoTheStringReplaceTrap() {
        // The buggy shortcut would be path.replace("apResource:", "") which is GLOBAL and would also
        // strip a second literal "apResource:" occurring inside the path. Index-based parsing must not.
        String ref = "apResource:abc:dir/apResource:literal.txt";
        assertEquals("abc", FileResolver.extractApId(ref));
        assertEquals("dir/apResource:literal.txt", FileResolver.extractApRelativePath(ref));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsReferenceWithoutRelativePath() {
        FileResolver.extractApRelativePath("apResource:abc");
    }

    @Test
    public void normalisesRelativePath() {
        assertEquals("scripts/kw.groovy", FileResolver.normalizeApRelativePath("./scripts/kw.groovy"));
        assertEquals("scripts/kw.groovy", FileResolver.normalizeApRelativePath("scripts\\kw.groovy"));
        assertEquals("scripts/kw.groovy", FileResolver.normalizeApRelativePath("/scripts/kw.groovy"));
        assertEquals("scripts/kw.groovy", FileResolver.normalizeApRelativePath("scripts/./kw.groovy"));
        assertEquals("kw.groovy", FileResolver.normalizeApRelativePath("scripts/../kw.groovy"));
    }

    /**
     * The exception type is part of the contract, not an implementation detail: the services exposing
     * the apResource endpoints map {@link IllegalArgumentException} to a 400. A bare
     * {@code RuntimeException} would fall through their catch clauses and be reported as a 500.
     */
    @Test
    public void rejectsTraversalEscape() {
        assertThrowsIllegalArgument(() -> FileResolver.normalizeApRelativePath("../secret"));
        assertThrowsIllegalArgument(() -> FileResolver.normalizeApRelativePath("scripts/../../secret"));
        assertThrowsIllegalArgument(() -> FileResolver.normalizeApRelativePath("a/b/../../../c"));
        assertThrowsIllegalArgument(() -> FileResolver.normalizeApRelativePath(""));
    }

    @Test
    public void resolveRoutesToProviderWithParsedArgs() {
        List<String[]> calls = new ArrayList<>();
        File sentinel = new File("materialised.groovy");
        FileResolver fileResolver = new FileResolver(null);
        fileResolver.setApResourceProvider((apId, relativePath) -> {
            calls.add(new String[]{apId, relativePath});
            return sentinel;
        });

        File resolved = fileResolver.resolve("apResource:abc:scripts/kw.groovy");

        assertSame(sentinel, resolved);
        assertEquals(1, calls.size());
        assertEquals("abc", calls.get(0)[0]);
        assertEquals("scripts/kw.groovy", calls.get(0)[1]);
    }

    @Test
    public void resolveThrowsWhenNoProviderConfigured() {
        FileResolver fileResolver = new FileResolver(null);
        assertThrowsRuntime(() -> fileResolver.resolve("apResource:abc:scripts/kw.groovy"));
    }

    private static void assertThrowsRuntime(Runnable r) {
        try {
            r.run();
            fail("Expected a RuntimeException");
        } catch (RuntimeException expected) {
            // ok
        }
    }

    private static void assertThrowsIllegalArgument(Runnable r) {
        try {
            r.run();
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
