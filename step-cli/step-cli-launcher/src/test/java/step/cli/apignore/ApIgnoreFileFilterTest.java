package step.cli.apignore;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class ApIgnoreFileFilterTest {

    @Test
    public void testApIgnoreFilter() throws IOException, URISyntaxException {
        URL resource = ApIgnoreFileFilterTest.class.getResource(".apignore");
        File file = Paths.get(resource.toURI()).toFile();
        Path rootPath = Paths.get("").toAbsolutePath();
        ApIgnoreFileFilter apIgnoreFileFilter = new ApIgnoreFileFilter(rootPath, file.toPath());

        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), ".dotIgnoreFile.txt")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "folder/.dotIgnoreFile.txt")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "folder/sub-folder/.dotIgnoreFile.txt")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "/root-folder/.dotIgnoreFile.txt")));

        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "ignoreFile.txt")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "folder/ignoreFile.txt")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "folder/sub-folder/ignoreFile.txt")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), ".rootDotIgnoreFile.txt")));
        assertTrue(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "folder/.rootDotIgnoreFile.txt")));

        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "ignore-folder-pattern/anything")));
        assertTrue(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "ignore-folder-pattern-for-file")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "src/test/resources/step/cli/apignore/folder/any-sub-folder/file.txt")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "folder/sub-folder/any-sub-folder/file.txt")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "/root-ignore-folder")));
        assertTrue(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "/sub/root-ignore-folder")));



        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "folder/sub-folder/app.exe")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "star.exe")));
        assertTrue(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "not-excluded-extension.java")));

        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "/any-nested-folder")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "/folder/any-nested-folder")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "/folder/sub/any-nested-folder")));

        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "/folder/test/sub/sub/aa")));
        assertFalse(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "/folder/test/aa")));
        assertTrue(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "/folder/test2/aa")));
        assertTrue(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "/folder/sub/test/test.txt")));

        assertTrue(apIgnoreFileFilter.accept(Path.of(rootPath.toString(), "somefile")));
    }

}