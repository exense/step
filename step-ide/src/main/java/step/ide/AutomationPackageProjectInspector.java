package step.ide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.JavaAutomationPackageArchive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspects the surroundings of an automation package directory to detect the ones belonging to a Java project.
 * <p>
 * The keywords and plans such a project defines in code with annotations are discovered when the project is built
 * and deployed, not by the IDE, which only knows what the YAML files declare. The inspection never fails: what cannot
 * be read is ignored.
 */
public class AutomationPackageProjectInspector {

    private static final Logger logger = LoggerFactory.getLogger(AutomationPackageProjectInspector.class);

    private static final List<String> BUILD_FILES = List.of("pom.xml", "build.gradle", "build.gradle.kts");
    private static final String SOURCES_DIRECTORY = "src";
    private static final String RESOURCES_DIRECTORY = "resources";
    private static final String MAIN_SOURCE_SET = "main";
    private static final List<String> CODE_DIRECTORIES = List.of("java", "kotlin", "groovy");
    private static final Set<String> CODE_FILE_EXTENSIONS = Set.of(".java", ".kt", ".groovy");
    private static final Pattern ANNOTATION = Pattern.compile("@(?:Keyword|Plan)\\b");

    private static final int MAX_SCANNED_FILES = 5000;
    private static final long MAX_SCANNED_FILE_SIZE = 1024 * 1024;

    private AutomationPackageProjectInspector() {
    }

    /**
     * @return the resources directory holding the automation package descriptor of the Java project the given
     * directory is the root of, if any
     */
    public static Optional<Path> findResourcesDirectoryWithDescriptor(Path projectDirectory) {
        try {
            Path resources = projectDirectory.resolve(SOURCES_DIRECTORY).resolve(MAIN_SOURCE_SET).resolve(RESOURCES_DIRECTORY);
            boolean hasDescriptor = JavaAutomationPackageArchive.METADATA_FILES.stream()
                .anyMatch(fileName -> Files.isRegularFile(resources.resolve(fileName)));
            return hasDescriptor ? Optional.of(resources) : Optional.empty();
        } catch (RuntimeException e) {
            logger.debug("Unable to look for an automation package descriptor in the resources of {}", projectDirectory, e);
            return Optional.empty();
        }
    }

    /**
     * @return the warnings to give to the user opening the given automation package directory, empty if there is none
     */
    public static List<String> inspect(Path automationPackageDirectory) {
        List<String> warnings = new ArrayList<>();
        try {
            Path directory = automationPackageDirectory.toAbsolutePath().normalize();
            inspectResourcesDirectory(directory, warnings);
            inspectBuildOutputDirectory(directory, warnings);
        } catch (RuntimeException e) {
            logger.debug("Unable to inspect the project of the automation package {}", automationPackageDirectory, e);
        }
        return warnings;
    }

    /**
     * Warns when the directory is the resources directory (src/[source set]/resources) of a Java project defining
     * keywords or plans in code
     */
    private static void inspectResourcesDirectory(Path directory, List<String> warnings) {
        Path sourceSetDirectory = directory.getParent();
        Path sourcesDirectory = sourceSetDirectory == null ? null : sourceSetDirectory.getParent();
        Path projectDirectory = sourcesDirectory == null ? null : sourcesDirectory.getParent();
        if (projectDirectory == null || !hasName(directory, RESOURCES_DIRECTORY) || !hasName(sourcesDirectory, SOURCES_DIRECTORY)
            || !isInBuildProject(projectDirectory)) {
            return;
        }
        int annotations = 0;
        for (String codeDirectory : CODE_DIRECTORIES) {
            annotations += countAnnotations(sourceSetDirectory.resolve(codeDirectory));
        }
        if (annotations > 0) {
            warnings.add("This Automation Package is part of a Java project. Keywords and plans defined in code with annotations ("
                + annotations + " found in " + projectDirectory.relativize(sourceSetDirectory) + ") are not available in the Studio:"
                + " they are not listed, and plans calling them cannot be executed here.");
        }
    }

    /**
     * Warns when the directory is the build output of a Maven (target/classes) or Gradle (build/resources/[source set])
     * project
     */
    private static void inspectBuildOutputDirectory(Path directory, List<String> warnings) {
        Path parent = directory.getParent();
        if (parent == null) {
            return;
        }
        Path projectDirectory = null;
        if (hasName(parent, "target") && (hasName(directory, "classes") || hasName(directory, "test-classes"))) {
            projectDirectory = parent.getParent();
        } else if (hasName(parent, RESOURCES_DIRECTORY) && hasName(parent.getParent(), "build")) {
            projectDirectory = parent.getParent().getParent();
        }
        if (projectDirectory != null && isInBuildProject(projectDirectory)) {
            warnings.add("This directory is the build output of a Java project: the changes made here are lost the next time"
                + " the project is built. Open the Automation Package from the resources of the project instead, for instance "
                + Path.of(SOURCES_DIRECTORY, MAIN_SOURCE_SET, RESOURCES_DIRECTORY) + ".");
        }
    }

    private static boolean hasName(Path path, String name) {
        return path != null && path.getFileName() != null && path.getFileName().toString().equals(name);
    }

    /**
     * @return true if the given directory or one of its ancestors holds a build file. The ancestors are considered for
     * the modules of a multi-module project, the ones which cannot be read are skipped
     */
    private static boolean isInBuildProject(Path directory) {
        for (Path current = directory; current != null; current = current.getParent()) {
            for (String buildFile : BUILD_FILES) {
                try {
                    if (Files.isRegularFile(current.resolve(buildFile))) {
                        return true;
                    }
                } catch (RuntimeException e) {
                    logger.debug("Unable to look for {} in {}", buildFile, current, e);
                }
            }
        }
        return false;
    }

    /**
     * @return the number of keyword and plan annotations found in the code files of the given directory. The scan is
     * bounded, and skips what it cannot read
     */
    private static int countAnnotations(Path codeDirectory) {
        int[] counters = new int[2]; // annotations, scanned files
        try {
            if (!Files.isDirectory(codeDirectory)) {
                return 0;
            }
            Files.walkFileTree(codeDirectory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (counters[1] >= MAX_SCANNED_FILES) {
                        return FileVisitResult.TERMINATE;
                    }
                    if (isCodeFile(file) && attributes.size() <= MAX_SCANNED_FILE_SIZE) {
                        counters[1]++;
                        counters[0] += countAnnotationsInFile(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | RuntimeException e) {
            logger.debug("Unable to scan {} for annotated keywords and plans", codeDirectory, e);
        }
        return counters[0];
    }

    private static boolean isCodeFile(Path file) {
        String fileName = file.getFileName().toString();
        return CODE_FILE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private static int countAnnotationsInFile(Path file) {
        try {
            // ISO-8859-1 decodes any content, the annotations looked for being plain ASCII
            Matcher matcher = ANNOTATION.matcher(Files.readString(file, StandardCharsets.ISO_8859_1));
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            return count;
        } catch (IOException | RuntimeException e) {
            return 0;
        }
    }
}
