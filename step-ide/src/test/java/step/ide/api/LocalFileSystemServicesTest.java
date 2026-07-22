package step.ide.api;

import jakarta.ws.rs.WebApplicationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LocalFileSystemServicesTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private LocalFileSystemServices service;
    private Path homePath;

    @Before
    public void setUp() throws IOException {
        service = new LocalFileSystemServices();
        homePath = tempFolder.getRoot().toPath();

        service.home = homePath;

        tempFolder.newFolder("DirB");
        tempFolder.newFolder("dirA");

        tempFolder.newFile("zebra.txt");
        tempFolder.newFile("äpfel.txt");

        // Write exactly 12 bytes to test file size
        Path applePath = tempFolder.newFile("apple.txt").toPath();
        Files.write(applePath, "Hello World!".getBytes());

        // Create a hidden file and ensure it works cross-platform
        Path hiddenFile = tempFolder.newFile(".hidden.txt").toPath();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            Files.setAttribute(hiddenFile, "dos:hidden", true);
        }
    }

    @Test
    public void testListDefaultHomeDirectory() {
        LocalFileSystemServices.DirectoryListing result = service.listDirectory(null, false, false, false);

        assertNotNull(result);
        assertEquals(homePath.toAbsolutePath().toString(), result.currentPath());

        List<LocalFileSystemServices.FileDescriptor> items = result.items();
        assertEquals(5, items.size());

        // Verify directories are always first and alphabetized (case-insensitive)
        assertEquals("dirA", items.get(0).name());
        assertTrue(items.get(0).isDirectory());

        assertEquals("DirB", items.get(1).name());
        assertTrue(items.get(1).isDirectory());

        // Verify dynamic collation order for files
        Collator testCollator = Collator.getInstance();
        testCollator.setStrength(Collator.SECONDARY);
        List<String> expectedFiles = Arrays.asList("apple.txt", "äpfel.txt", "zebra.txt");
        expectedFiles.sort(testCollator);

        assertEquals(expectedFiles.get(0), items.get(2).name());
        assertEquals(expectedFiles.get(1), items.get(3).name());
        assertEquals(expectedFiles.get(2), items.get(4).name());

        // Verify file sizes and types
        LocalFileSystemServices.FileDescriptor appleDescriptor = items.stream()
            .filter(fd -> fd.name().equals("apple.txt"))
            .findFirst()
            .orElseThrow();

        assertEquals(12L, appleDescriptor.size());
        assertTrue(appleDescriptor.isFile());
        assertFalse(appleDescriptor.isDirectory());
        assertFalse(appleDescriptor.isHidden());

        // Zebra is empty
        LocalFileSystemServices.FileDescriptor zebraDescriptor = items.stream()
            .filter(fd -> fd.name().equals("zebra.txt"))
            .findFirst()
            .orElseThrow();
        assertEquals(0L, zebraDescriptor.size());
    }

    @Test
    public void testListShowHiddenFiles() {
        LocalFileSystemServices.DirectoryListing result = service.listDirectory("", true, false, false);

        assertEquals(6, result.items().size());

        boolean foundHidden = result.items().stream()
            .anyMatch(LocalFileSystemServices.FileDescriptor::isHidden);
        assertTrue("Hidden file should be present in the results", foundHidden);
    }

    @Test
    public void testListFilesOnly() {
        LocalFileSystemServices.DirectoryListing result = service.listDirectory(homePath.toString(), false, true, false);

        assertEquals(3, result.items().size());
        for (LocalFileSystemServices.FileDescriptor item : result.items()) {
            assertTrue(item.isFile());
            assertFalse(item.isDirectory());
        }
    }

    @Test
    public void testListDirsOnly() {
        LocalFileSystemServices.DirectoryListing result = service.listDirectory(homePath.toString(), false, false, true);

        assertEquals(2, result.items().size());
        for (LocalFileSystemServices.FileDescriptor item : result.items()) {
            assertTrue(item.isDirectory());
            assertFalse(item.isFile());
            // Directory sizes vary by OS (often 0, 4096, etc.), but we can ensure the property maps
            assertTrue("Directory size should map without exception", item.size() >= 0);
        }
    }

    @Test
    public void testConflictingFiltersThrowsException() {
        try {
            service.listDirectory(homePath.toString(), false, true, true);
            fail("Expected WebApplicationException to be thrown");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void testInvalidDirectoryPath() {
        Path fakePath = homePath.resolve("does_not_exist");
        try {
            service.listDirectory(fakePath.toString(), false, false, false);
            fail("Expected WebApplicationException for non-existent path");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void testPathIsFileNotDirectory() {
        Path filePath = homePath.resolve("zebra.txt");
        try {
            service.listDirectory(filePath.toString(), false, false, false);
            fail("Expected WebApplicationException because path is a file");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void testPathTraversalNormalization() {
        String traversalPath = homePath.resolve("dirA").resolve("..").toString();
        LocalFileSystemServices.DirectoryListing result = service.listDirectory(traversalPath, false, false, false);

        assertEquals(homePath.toAbsolutePath().toString(), result.currentPath());
        assertEquals(5, result.items().size());
    }

    @Test
    public void testCreateDirectorySuccess() {
        LocalFileSystemServices.CreateDirectoryRequest request =
            new LocalFileSystemServices.CreateDirectoryRequest(homePath.toString(), "new_test_folder");

        LocalFileSystemServices.FileDescriptor result = service.createDirectory(request);

        // Verify the API response
        assertNotNull(result);
        assertEquals("new_test_folder", result.name());
        assertTrue(result.isDirectory());
        assertFalse(result.isFile());

        // Verify the filesystem was actually modified
        Path expectedPath = homePath.resolve("new_test_folder");
        assertTrue("Directory should exist on disk", Files.exists(expectedPath));
        assertTrue("Path should be a directory", Files.isDirectory(expectedPath));
    }

    @Test
    public void testCreateDirectoryMissingRequiredFields() {
        LocalFileSystemServices.CreateDirectoryRequest[] invalidRequests = {
            null,
            new LocalFileSystemServices.CreateDirectoryRequest(null, "folder"),
            new LocalFileSystemServices.CreateDirectoryRequest(homePath.toString(), null),
            new LocalFileSystemServices.CreateDirectoryRequest(homePath.toString(), "   ") // blank
        };

        for (LocalFileSystemServices.CreateDirectoryRequest req : invalidRequests) {
            try {
                service.createDirectory(req);
                fail("Expected WebApplicationException for invalid payload: " + req);
            } catch (WebApplicationException e) {
                assertEquals(400, e.getResponse().getStatus());
            }
        }
    }

    @Test
    public void testCreateDirectorySecurityInvalidCharacters() {
        String[] maliciousNames = {
            "my/folder",
            "my\\folder",
            "..",
            "."
        };

        for (String badName : maliciousNames) {
            LocalFileSystemServices.CreateDirectoryRequest req =
                new LocalFileSystemServices.CreateDirectoryRequest(homePath.toString(), badName);
            try {
                service.createDirectory(req);
                fail("Expected WebApplicationException for malicious name: " + badName);
            } catch (WebApplicationException e) {
                assertEquals(400, e.getResponse().getStatus());
            }
        }
    }

    @Test
    public void testCreateDirectoryParentDoesNotExist() {
        Path fakeParent = homePath.resolve("ghost_dir");
        LocalFileSystemServices.CreateDirectoryRequest req =
            new LocalFileSystemServices.CreateDirectoryRequest(fakeParent.toString(), "new_folder");

        try {
            service.createDirectory(req);
            fail("Expected WebApplicationException because parent doesn't exist");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void testCreateDirectoryParentIsAFile() {
        // "zebra.txt" was created in the setUp() method
        Path fileParent = homePath.resolve("zebra.txt");
        LocalFileSystemServices.CreateDirectoryRequest req =
            new LocalFileSystemServices.CreateDirectoryRequest(fileParent.toString(), "new_folder");

        try {
            service.createDirectory(req);
            fail("Expected WebApplicationException because parent is a file, not a directory");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
    }

    @Test
    public void testCreateDirectoryAlreadyExistsConflict() {
        // "dirA" is a directory created in setUp()
        LocalFileSystemServices.CreateDirectoryRequest reqDir =
            new LocalFileSystemServices.CreateDirectoryRequest(homePath.toString(), "dirA");

        try {
            service.createDirectory(reqDir);
            fail("Expected 409 Conflict for existing directory");
        } catch (WebApplicationException e) {
            assertEquals(409, e.getResponse().getStatus());
        }

        // "zebra.txt" is a file created in setUp() - conflict applies to files sharing the name too
        LocalFileSystemServices.CreateDirectoryRequest reqFile =
            new LocalFileSystemServices.CreateDirectoryRequest(homePath.toString(), "zebra.txt");

        try {
            service.createDirectory(reqFile);
            fail("Expected 409 Conflict for existing file");
        } catch (WebApplicationException e) {
            assertEquals(409, e.getResponse().getStatus());
        }
    }
}
