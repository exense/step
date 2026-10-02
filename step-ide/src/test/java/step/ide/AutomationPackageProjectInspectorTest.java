package step.ide;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AutomationPackageProjectInspectorTest {

    private static final String ANNOTATED_KEYWORDS = "public class MyKeywords {\n"
        + "    @Keyword\n    public void first() {}\n"
        + "    @Keyword(name = \"Second\")\n    public void second() {}\n"
        + "    @Plan\n    public void plan() {}\n"
        + "    @Plans\n    @KeywordGroup\n    public void notCounted() {}\n"
        + "}\n";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path project;
    private Path resources;

    @Before
    public void setUp() throws IOException {
        project = tempFolder.newFolder("project").toPath();
        resources = Files.createDirectories(project.resolve("src/main/resources"));
    }

    @Test
    public void descriptorIsFoundInTheResourcesOfAProjectRoot() throws IOException {
        assertEquals(Optional.empty(), AutomationPackageProjectInspector.findResourcesDirectoryWithDescriptor(project));

        Files.writeString(resources.resolve("automation-package.yml"), "name: test\n");
        assertEquals(Optional.of(resources), AutomationPackageProjectInspector.findResourcesDirectoryWithDescriptor(project));
    }

    @Test
    public void descriptorWithYamlExtensionIsFound() throws IOException {
        Files.writeString(resources.resolve("automation-package.yaml"), "name: test\n");
        assertEquals(Optional.of(resources), AutomationPackageProjectInspector.findResourcesDirectoryWithDescriptor(project));
    }

    @Test
    public void descriptorLookupToleratesAMissingDirectory() {
        assertEquals(Optional.empty(), AutomationPackageProjectInspector.findResourcesDirectoryWithDescriptor(project.resolve("missing")));
    }

    @Test
    public void javaProjectWithAnnotatedCodeIsWarned() throws IOException {
        Files.writeString(project.resolve("pom.xml"), "<project/>");
        writeCode("src/main/java/com/acme/MyKeywords.java", ANNOTATED_KEYWORDS);
        writeCode("src/main/kotlin/com/acme/Other.kt", "@Keyword fun other() {}\n");
        // Neither a code file nor the same source set
        writeCode("src/main/java/com/acme/notes.txt", "@Keyword");
        writeCode("src/test/java/com/acme/TestKeywords.java", "@Keyword");

        List<String> warnings = AutomationPackageProjectInspector.inspect(resources);

        assertEquals(1, warnings.size());
        String warning = warnings.get(0);
        assertTrue(warning, warning.startsWith("This Automation Package is part of a Java project."));
        assertTrue(warning, warning.contains("(4 found in " + Path.of("src", "main") + ")"));
    }

    @Test
    public void buildFileOfAParentModuleIsConsidered() throws IOException {
        Files.writeString(project.resolve("build.gradle.kts"), "");
        Path module = Files.createDirectories(project.resolve("modules/my-module"));
        Path moduleResources = Files.createDirectories(module.resolve("src/main/resources"));
        Files.createDirectories(module.resolve("src/main/java"));
        Files.writeString(module.resolve("src/main/java/MyKeywords.java"), ANNOTATED_KEYWORDS);

        assertEquals(1, AutomationPackageProjectInspector.inspect(moduleResources).size());
    }

    @Test
    public void javaProjectWithoutAnnotatedCodeIsNotWarned() throws IOException {
        Files.writeString(project.resolve("pom.xml"), "<project/>");
        writeCode("src/main/java/com/acme/Helper.java", "public class Helper {}\n");

        assertTrue(AutomationPackageProjectInspector.inspect(resources).isEmpty());
    }

    @Test
    public void resourcesDirectoryOutsideOfABuildProjectIsNotWarned() throws IOException {
        writeCode("src/main/java/com/acme/MyKeywords.java", ANNOTATED_KEYWORDS);

        assertTrue(AutomationPackageProjectInspector.inspect(resources).isEmpty());
    }

    @Test
    public void plainDirectoryIsNotWarned() throws IOException {
        Files.writeString(project.resolve("pom.xml"), "<project/>");
        writeCode("src/main/java/com/acme/MyKeywords.java", ANNOTATED_KEYWORDS);

        assertTrue(AutomationPackageProjectInspector.inspect(project).isEmpty());
        assertTrue(AutomationPackageProjectInspector.inspect(project.resolve("missing")).isEmpty());
    }

    @Test
    public void mavenBuildOutputIsWarned() throws IOException {
        Files.writeString(project.resolve("pom.xml"), "<project/>");
        Path classes = Files.createDirectories(project.resolve("target/classes"));

        List<String> warnings = AutomationPackageProjectInspector.inspect(classes);

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0), warnings.get(0).startsWith("This directory is the build output of a Java project"));
    }

    @Test
    public void gradleBuildOutputIsWarned() throws IOException {
        Files.writeString(project.resolve("build.gradle"), "");
        Path output = Files.createDirectories(project.resolve("build/resources/main"));

        assertEquals(1, AutomationPackageProjectInspector.inspect(output).size());
    }

    @Test
    public void buildOutputLookalikeOutsideOfABuildProjectIsNotWarned() throws IOException {
        Path classes = Files.createDirectories(project.resolve("target/classes"));

        assertFalse(Files.exists(project.resolve("pom.xml")));
        assertTrue(AutomationPackageProjectInspector.inspect(classes).isEmpty());
    }

    private void writeCode(String relativePath, String content) throws IOException {
        Path file = project.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
