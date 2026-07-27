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

import ch.exense.commons.io.FileHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.attachments.ApResourceNotFoundException;
import step.automation.packages.accessor.AutomationPackageAccessor;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class AutomationPackageResourceProviderTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File cacheRoot;
    private File archiveZip;

    @Before
    public void setUp() throws Exception {
        cacheRoot = tmp.newFolder("AP_cache");
        File source = tmp.newFolder("ap-source");
        Files.createDirectories(new File(source, "scripts").toPath());
        Files.writeString(new File(source, "scripts/kw.groovy").toPath(), "println 'kw'");
        archiveZip = new File(tmp.getRoot(), "ap.zip");
        FileHelper.zip(source, archiveZip);
    }

    /**
     * Builds an {@link AutomationPackageAccessor} that only implements {@code get(String)} (returning
     * an {@link AutomationPackage} from the given map). Avoids hand-implementing the whole accessor
     * interface for a focused unit test.
     */
    private AutomationPackageAccessor accessorReturning(Map<String, AutomationPackage> byId) {
        return (AutomationPackageAccessor) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{AutomationPackageAccessor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("get") && args != null && args.length == 1 && args[0] instanceof String) {
                        return byId.get((String) args[0]);
                    }
                    if (method.getName().equals("toString")) {
                        return "stub-accessor";
                    }
                    return null;
                });
    }

    private AutomationPackage apWithArchiveReference(String reference) {
        AutomationPackage ap = new AutomationPackage();
        ap.setAutomationPackageResource(reference);
        return ap;
    }

    private static AutomationPackageArchive newArchive(File file) {
        try {
            return new JavaAutomationPackageArchive(file, null, null);
        } catch (AutomationPackageReadingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void resolvesEntryThroughAccessorAndArchiveResolver() throws Exception {
        AutomationPackageAccessor accessor = accessorReturning(Map.of("apA", apWithArchiveReference("resource:archive-1")));
        AtomicReference<String> resolvedReference = new AtomicReference<>();
        Function<String, File> archiveResolver = ref -> {
            resolvedReference.set(ref);
            return archiveZip;
        };
        AutomationPackageResourceProvider provider =
                new AutomationPackageResourceProvider(cacheRoot, () -> accessor, archiveResolver, AutomationPackageResourceProviderTest::newArchive);

        File file = provider.resolve("apA", "scripts/kw.groovy");

        assertEquals("println 'kw'", Files.readString(file.toPath()));
        assertEquals("archive reference must be passed to the archive resolver", "resource:archive-1", resolvedReference.get());
    }

    @Test
    public void unknownApThrowsNotFound() {
        AutomationPackageAccessor accessor = accessorReturning(Map.of());
        AutomationPackageResourceProvider provider =
                new AutomationPackageResourceProvider(cacheRoot, () -> accessor, ref -> archiveZip, AutomationPackageResourceProviderTest::newArchive);
        try {
            provider.resolve("missing", "scripts/kw.groovy");
            fail("expected ApResourceNotFoundException");
        } catch (ApResourceNotFoundException expected) {
            assertTrue(expected.getMessage().contains("missing"));
        }
    }

    @Test
    public void missingAccessorThrows() {
        Supplier<AutomationPackageAccessor> noAccessor = () -> null;
        AutomationPackageResourceProvider provider =
                new AutomationPackageResourceProvider(cacheRoot, noAccessor, ref -> archiveZip, AutomationPackageResourceProviderTest::newArchive);
        try {
            provider.resolve("apA", "scripts/kw.groovy");
            fail("expected RuntimeException");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("AutomationPackageAccessor"));
        }
    }

    @Test
    public void apWithoutArchiveReferenceThrowsNotFound() {
        AutomationPackageAccessor accessor = accessorReturning(Map.of("apA", apWithArchiveReference(null)));
        AutomationPackageResourceProvider provider =
                new AutomationPackageResourceProvider(cacheRoot, () -> accessor, ref -> archiveZip, AutomationPackageResourceProviderTest::newArchive);
        try {
            provider.resolve("apA", "scripts/kw.groovy");
            fail("expected ApResourceNotFoundException");
        } catch (ApResourceNotFoundException expected) {
            assertTrue(expected.getMessage().contains("no archive resource"));
        }
    }
}
