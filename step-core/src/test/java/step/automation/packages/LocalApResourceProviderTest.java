/*
 * Copyright (C) 2024, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

package step.automation.packages;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Resolution of the {@code apResource:local:} references of the automation package open in the editor.
 * The package is an exploded directory, so the file is served where it lies - which is what lets the
 * script editor write back into the user's sources.
 */
public class LocalApResourceProviderTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final AtomicReference<Path> openPackage = new AtomicReference<>();
    private File scriptFile;
    private LocalApResourceProvider provider;

    @Before
    public void setUp() throws IOException {
        File directory = tmp.newFolder("my-ap");
        scriptFile = new File(directory, "scripts/kw.groovy");
        Files.createDirectories(scriptFile.getParentFile().toPath());
        Files.writeString(scriptFile.toPath(), "println 'hello'");
        openPackage.set(directory.toPath());
        provider = new LocalApResourceProvider(openPackage::get, null);
    }

    @Test
    public void resolvesTheFileInPlace() {
        File resolved = provider.resolve("local", "scripts/kw.groovy");

        // the very file, not a materialised copy: the editor writes through it
        assertEquals(scriptFile.getAbsolutePath(), resolved.getAbsolutePath());
        assertTrue(resolved.exists());
    }

    @Test
    public void normalisesTheRelativePath() {
        assertEquals(scriptFile.getAbsolutePath(), provider.resolve("local", "./scripts/kw.groovy").getAbsolutePath());
        assertEquals(scriptFile.getAbsolutePath(), provider.resolve("local", "scripts\\kw.groovy").getAbsolutePath());
    }

    /**
     * An automation package is self-contained; a reference reaching out of it is refused rather than
     * resolved.
     */
    @Test
    public void refusesToLeaveTheAutomationPackage() {
        assertThrows(IllegalArgumentException.class, () -> provider.resolve("local", "../secret.txt"));
    }

    @Test
    public void reportsAMissingFileAsNotFound() {
        assertThrows(ApResourceNotFoundException.class, () -> provider.resolve("local", "scripts/missing.groovy"));
    }

    /**
     * Not a bad reference - there is simply nothing to resolve it against yet.
     */
    @Test
    public void failsWhenNoAutomationPackageIsOpen() {
        openPackage.set(null);

        assertThrows(IllegalStateException.class, () -> provider.resolve("local", "scripts/kw.groovy"));
    }

    /**
     * The editor may run next to deployed packages; those keep resolving through the provider this one
     * wraps.
     */
    @Test
    public void delegatesEveryOtherAutomationPackage() {
        File deployed = new File("deployed.groovy");
        LocalApResourceProvider delegating = new LocalApResourceProvider(openPackage::get,
            (apId, relativePath) -> deployed);

        assertEquals(deployed, delegating.resolve("66c1f0f0f0f0f0f0f0f0f0f0", "scripts/kw.groovy"));
    }

    @Test
    public void failsOnADeployedPackageWithoutADelegate() {
        RuntimeException e = assertThrows(RuntimeException.class,
            () -> provider.resolve("66c1f0f0f0f0f0f0f0f0f0f0", "scripts/kw.groovy"));

        assertTrue(e.getMessage(), e.getMessage().contains("66c1f0f0f0f0f0f0f0f0f0f0"));
    }
}
