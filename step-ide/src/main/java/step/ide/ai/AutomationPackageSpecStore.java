package step.ide.ai;

import step.automation.packages.yaml.YamlFragmentFilenames;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Stores the plain text specs entered by the user as <code>specs/&lt;test case name&gt;.md</code> inside the
 * automation package directory. The spec is the durable record of what the user asked for and is what the
 * "Regenerate with AI" action reads back.
 * <p>
 * These files are deliberately not managed by the automation package fragment manager: they are not fragments
 * referenced by the descriptor, they are not read when the package is parsed, and they may be rewritten by the AI
 * agent. Registering them there would drag them into the concurrent edit detection and fight the agent's own writes.
 */
public class AutomationPackageSpecStore {

    public static final String SPECS_DIRECTORY = "specs";
    public static final String SPEC_FILE_EXTENSION = ".md";

    private final Path apRoot;

    public AutomationPackageSpecStore(java.io.File apRoot) {
        this.apRoot = apRoot.toPath().toAbsolutePath().normalize();
    }

    /**
     * @return the absolute path of the spec file of the given test case, guaranteed to be directly inside the specs
     * directory
     */
    public Path specPath(String testCaseName) {
        if (testCaseName == null || testCaseName.isBlank()) {
            throw new IllegalArgumentException("The test case name must not be empty");
        }
        Path specsDirectory = apRoot.resolve(SPECS_DIRECTORY).normalize();
        // the same sanitization as for the plan fragments, so that specs and plans map onto each other by name
        String fileName = YamlFragmentFilenames.sanitizeFilename(testCaseName + SPEC_FILE_EXTENSION);
        Path resolved = specsDirectory.resolve(fileName).normalize();
        // defense in depth: the sanitization already percent encodes separators, but never let a name escape
        if (!specsDirectory.equals(resolved.getParent())) {
            throw new IllegalArgumentException("Invalid test case name '" + testCaseName + "': it would resolve outside of the "
                + SPECS_DIRECTORY + " directory");
        }
        return resolved;
    }

    public Optional<String> read(String testCaseName) throws IOException {
        Path path = specPath(testCaseName);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
    }

    public Path write(String testCaseName, String content) throws IOException {
        Path path = specPath(testCaseName);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
        return path;
    }

    /**
     * @return the path of the given spec file relative to the automation package root, using forward slashes
     * (e.g. <code>specs/My test case.md</code>)
     */
    public String relativize(Path specFile) {
        return apRoot.relativize(specFile).toString().replace('\\', '/');
    }
}
