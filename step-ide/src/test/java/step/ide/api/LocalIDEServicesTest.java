package step.ide.api;

import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.core.deployment.ControllerServiceException;
import step.core.filebrowser.DirectoryListing;
import step.core.filebrowser.FileDescriptor;
import step.ide.LocalIDEState;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The browsing itself is covered by {@code ApResourceBrowserTest}; what is asserted here is what the
 * IDE endpoints add to it - the AP-root relative references the editor expects, the status codes, and
 * the fact that no automation package being open is reported as a state problem rather than a bad
 * request.
 */
public class LocalIDEServicesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File apDirectory;

    @Before
    public void setUp() throws IOException {
        apDirectory = tmp.newFolder("my-ap");
        writeFile("automation-package.yml", "schemaVersion: 1.0.0\nname: \"myAp\"\n");
        writeFile("data/pool.csv", "a,b,c");
        writeFile("scripts/kw.groovy", "println 'hello'");
    }

    private void writeFile(String relativePath, String content) throws IOException {
        File file = new File(apDirectory, relativePath);
        Files.createDirectories(file.getParentFile().toPath());
        Files.writeString(file.toPath(), content);
    }

    private static List<String> names(DirectoryListing listing) {
        return listing.entries().stream().map(FileDescriptor::name).collect(Collectors.toList());
    }

    @Test
    public void listsTheRootOfTheAutomationPackage() {
        DirectoryListing listing = LocalIDEServices.browse(apDirectory, null, false, false);

        assertEquals("", listing.path());
        assertNull(listing.parentPath());
        assertEquals(List.of("data", "scripts", "automation-package.yml"), names(listing));
    }

    /**
     * The whole point of the IDE variant: the editor writes the picked value into the YAML descriptor,
     * which knows no {@code apId}, so the reference must be the plain relative path.
     */
    @Test
    public void producesPlainRelativeReferences() {
        DirectoryListing listing = LocalIDEServices.browse(apDirectory, "data", false, false);

        assertEquals("data", listing.resourceReference());
        assertEquals("data/pool.csv", listing.entries().get(0).resourceReference());
    }

    @Test
    public void opensTheParentDirectoryWhenThePathIsAFile() {
        DirectoryListing listing = LocalIDEServices.browse(apDirectory, "scripts/kw.groovy", false, false);

        assertEquals("scripts", listing.path());
        assertEquals(List.of("kw.groovy"), names(listing));
    }

    @Test
    public void filtersTheListedEntries() {
        assertEquals(List.of("automation-package.yml"),
            names(LocalIDEServices.browse(apDirectory, null, true, false)));
        assertEquals(List.of("data", "scripts"),
            names(LocalIDEServices.browse(apDirectory, null, false, true)));
    }

    @Test
    public void rejectsBothFiltersAtOnce() {
        assertStatus(400, () -> LocalIDEServices.browse(apDirectory, null, true, true));
    }

    @Test
    public void reportsAnUnknownPathAsNotFound() {
        assertStatus(404, () -> LocalIDEServices.browse(apDirectory, "data/missing.csv", false, false));
    }

    @Test
    public void servesTheContentOfAFile() throws Exception {
        Response response = LocalIDEServices.content(apDirectory, "data/pool.csv", false);

        assertEquals(200, response.getStatus());
        assertTrue(response.getHeaderString("content-disposition").contains("attachment; filename=\"pool.csv\""));
        assertEquals("a,b,c", readEntity(response));
    }

    @Test
    public void servesTheContentInline() {
        Response response = LocalIDEServices.content(apDirectory, "data/pool.csv", true);

        assertTrue(response.getHeaderString("content-disposition").startsWith("inline;"));
    }

    @Test
    public void contentRejectsAMissingPath() {
        assertStatus(400, () -> LocalIDEServices.content(apDirectory, "  ", false));
    }

    @Test
    public void contentReportsAnUnknownFileAsNotFound() {
        assertStatus(404, () -> LocalIDEServices.content(apDirectory, "data/missing.csv", false));
    }

    /**
     * A directory cannot be downloaded - a bad request, unlike a path that simply does not exist.
     */
    @Test
    public void contentRejectsADirectory() {
        assertStatus(400, () -> LocalIDEServices.content(apDirectory, "data", false));
    }

    /**
     * Not a 400: the request is well formed, the editor just has no automation package open yet.
     */
    @Test
    public void reportsThatNoAutomationPackageIsOpen() {
        // No package has been opened in this JVM. Closing one here is not an option: LocalIDEState is a
        // singleton and closeCurrentAutomationPackage() needs the collection factory that only a real
        // editor session creates - so assert the precondition rather than establish it.
        assertNull(LocalIDEState.get().getCurrentAutomationPackageDirectory());

        assertStatus(409, () -> new LocalIDEServices().browseAP(null, false, false));
        assertStatus(409, () -> new LocalIDEServices().getAPContent("data/pool.csv", false));
    }

    private static String readEntity(Response response) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        ((jakarta.ws.rs.core.StreamingOutput) response.getEntity()).write(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private static void assertStatus(int expectedStatus, Runnable call) {
        try {
            call.run();
            fail("expected a " + expectedStatus);
        } catch (ControllerServiceException e) {
            assertEquals(expectedStatus, e.getHttpErrorCode());
        }
    }
}
