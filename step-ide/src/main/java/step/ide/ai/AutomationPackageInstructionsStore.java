package step.ide.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads back the specs of the test cases of an automation package from <code>specs/instructions.json</code>.
 * <p>
 * That file is the serialized input of the run that generated the package (the {@code AiInput} payload handed to the
 * agent, with the test case names resolved against the codebase).
 */
public class AutomationPackageInstructionsStore {

    public static final String SPECS_DIRECTORY = "specs";
    public static final String INSTRUCTIONS_FILE = "instructions.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path apRoot;

    public AutomationPackageInstructionsStore(java.io.File apRoot) {
        this.apRoot = apRoot.toPath().toAbsolutePath().normalize();
    }

    /** @return the absolute path of the instructions file, whether or not it exists */
    public Path instructionsPath() {
        return apRoot.resolve(SPECS_DIRECTORY).resolve(INSTRUCTIONS_FILE).normalize();
    }

    /**
     * @return the path of the instructions file relative to the automation package root, using forward slashes
     * (i.e. <code>specs/instructions.json</code>)
     */
    public String relativeInstructionsPath() {
        return apRoot.relativize(instructionsPath()).toString().replace('\\', '/');
    }

    /**
     * @return the spec of the given test case as recorded by the run that generated it, empty when the package holds
     * no instructions file or none of its test cases carries that name
     * @throws IOException if the instructions file exists but cannot be read or is not valid JSON
     */
    public Optional<String> readSpec(String testCaseName) throws IOException {
        if (testCaseName == null || testCaseName.isBlank()) {
            throw new IllegalArgumentException("The test case name must not be empty");
        }
        Path path = instructionsPath();
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        JsonNode testCases = objectMapper.readTree(Files.readString(path, StandardCharsets.UTF_8)).path("testCases");
        for (JsonNode testCase : testCases) {
            // the name the agent resolved is the one the plan carries, so an exact match is the normal case; the
            // normalization only absorbs the whitespace and casing differences a hand edited file may introduce
            if (sameName(testCase.path("name").asText(null), testCaseName)) {
                JsonNode spec = testCase.path("spec");
                return spec.isTextual() ? Optional.of(spec.asText()) : Optional.empty();
            }
        }
        return Optional.empty();
    }

    private boolean sameName(String recorded, String requested) {
        return recorded != null && normalize(recorded).equals(normalize(requested));
    }

    private String normalize(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
