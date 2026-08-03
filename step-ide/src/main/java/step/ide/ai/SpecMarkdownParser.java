package step.ide.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a markdown document holding several test case specs into one entry per test case.
 * <p>
 * The canonical format, which is the one the AI agent consumes, uses one level 2 heading per test case, the heading
 * text being the test case name:
 * <pre>
 * ## Login with valid credentials
 * Open the login page and ...
 *
 * ## Login with invalid credentials
 * ...
 * </pre>
 */
public class SpecMarkdownParser {

    private static final String HEADING_PREFIX = "## ";

    public record ParsedSpec(String name, String spec) {
    }

    /**
     * @param markdown the raw markdown entered or uploaded by the user
     * @return one entry per level 2 heading, in document order
     * @throws IllegalArgumentException if the document contains no level 2 heading, or a heading without a name
     */
    public List<ParsedSpec> parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            throw new IllegalArgumentException("The specification must not be empty");
        }

        List<ParsedSpec> specs = new ArrayList<>();
        String currentName = null;
        StringBuilder currentSpec = new StringBuilder();

        for (String line : markdown.split("\r\n|\r|\n", -1)) {
            if (isHeading(line)) {
                if (currentName != null) {
                    specs.add(new ParsedSpec(currentName, currentSpec.toString().strip()));
                    currentSpec.setLength(0);
                }
                currentName = line.strip().substring(HEADING_PREFIX.length()).strip();
                if (currentName.isEmpty()) {
                    throw new IllegalArgumentException("A test case heading ('" + HEADING_PREFIX.strip() + "') must be followed by the test case name");
                }
            } else if (currentName != null) {
                currentSpec.append(line).append('\n');
            }
            // content before the first heading is ignored, it is not part of any test case
        }

        if (currentName == null) {
            throw new IllegalArgumentException("No test case found. Separate test cases with a '" + HEADING_PREFIX.strip()
                + " <test case name>' heading, one per test case.");
        }
        specs.add(new ParsedSpec(currentName, currentSpec.toString().strip()));
        return specs;
    }

    private boolean isHeading(String line) {
        String stripped = line.strip();
        return stripped.startsWith(HEADING_PREFIX) && !stripped.startsWith("###");
    }
}
