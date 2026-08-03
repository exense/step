package step.ide.ai;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AutomationPackageSpecStoreTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File apRoot;
    private AutomationPackageSpecStore specStore;

    @Before
    public void setUp() throws Exception {
        apRoot = temporaryFolder.newFolder("ap");
        specStore = new AutomationPackageSpecStore(apRoot);
    }

    @Test
    public void writesTheSpecUnderTheSpecsDirectory() throws Exception {
        Path written = specStore.write("Login flow", "Open the login page.");

        assertEquals(apRoot.toPath().resolve("specs").resolve("Login flow.md"), written);
        assertEquals("Open the login page.", Files.readString(written));
        assertEquals("specs/Login flow.md", specStore.relativize(written));
    }

    @Test
    public void readsBackWhatWasWritten() throws Exception {
        specStore.write("Login flow", "Open the login page.");

        assertEquals(Optional.of("Open the login page."), specStore.read("Login flow"));
    }

    @Test
    public void readingAMissingSpecReturnsEmpty() throws Exception {
        assertEquals(Optional.empty(), specStore.read("Never written"));
    }

    /**
     * The spec filename has to be sanitized exactly like the plan fragment filename, otherwise mapping a plan onto
     * its spec by name silently breaks.
     */
    @Test
    public void sanitizesNamesTheSameWayAsPlanFragments() throws Exception {
        Path written = specStore.write("A, B / C", "Do something.");

        assertEquals("A%2C B %2F C.md", written.getFileName().toString());
        assertEquals(apRoot.toPath().resolve("specs"), written.getParent());
        assertEquals(Optional.of("Do something."), specStore.read("A, B / C"));
    }

    @Test
    public void doesNotEscapeTheSpecsDirectory() throws Exception {
        for (String name : new String[]{"../evil", "..", ".", "a/b", "a\\b", "/etc/passwd", "C:\\windows\\system32"}) {
            Path path = specStore.specPath(name);
            assertEquals("name '" + name + "' escaped the specs directory", apRoot.toPath().resolve("specs"), path.getParent());
            assertTrue("name '" + name + "' escaped the AP root", path.startsWith(apRoot.toPath()));
        }
    }

    @Test
    public void writingATraversingNameStaysInsideTheSpecsDirectory() throws Exception {
        specStore.write("../evil", "nope");

        assertFalse(Files.exists(apRoot.toPath().getParent().resolve("evil.md")));
        assertTrue(Files.list(apRoot.toPath().resolve("specs")).findAny().isPresent());
    }

    @Test
    public void rejectsBlankNames() {
        assertThrows(IllegalArgumentException.class, () -> specStore.specPath(null));
        assertThrows(IllegalArgumentException.class, () -> specStore.specPath("   "));
    }
}
