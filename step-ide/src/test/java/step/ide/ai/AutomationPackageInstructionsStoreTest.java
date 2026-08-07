package step.ide.ai;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class AutomationPackageInstructionsStoreTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File apRoot;
    private AutomationPackageInstructionsStore instructionsStore;

    @Before
    public void setUp() throws Exception {
        apRoot = temporaryFolder.newFolder("ap");
        instructionsStore = new AutomationPackageInstructionsStore(apRoot);
    }

    @Test
    public void readsTheSpecOfTheRequestedTestCase() throws Exception {
        writeInstructions("""
            {
              "testCases": [
                {"index": 1, "mode": "create", "name": "Login flow", "spec": "Open the login page.", "hints": ""},
                {"index": 2, "mode": "regenerate", "name": "Checkout", "spec": "Order a MacBook.", "hints": "be quick"}
              ],
              "hints": "run headless"
            }
            """);

        assertEquals(Optional.of("Open the login page."), instructionsStore.readSpec("Login flow"));
        assertEquals(Optional.of("Order a MacBook."), instructionsStore.readSpec("Checkout"));
    }

    @Test
    public void readsBackTheSerializedInstructionsAsSubmitted() throws Exception {
        // the payload as the IDE submits it, before the agent resolves the names and adds its own fields
        writeInstructions("{\"testCases\":[{\"name\":\"Login flow\",\"spec\":\"Open the login page.\",\"mode\":\"create\",\"hints\":null}],\"hints\":null}");

        assertEquals(Optional.of("Open the login page."), instructionsStore.readSpec("Login flow"));
    }

    @Test
    public void matchesTheTestCaseNameRegardlessOfCasingAndWhitespace() throws Exception {
        writeInstructions("{\"testCases\":[{\"name\":\"Login flow\",\"spec\":\"Open the login page.\"}]}");

        assertEquals(Optional.of("Open the login page."), instructionsStore.readSpec("  login   FLOW "));
    }

    @Test
    public void readingAnUnknownTestCaseReturnsEmpty() throws Exception {
        writeInstructions("{\"testCases\":[{\"name\":\"Login flow\",\"spec\":\"Open the login page.\"}]}");

        assertEquals(Optional.empty(), instructionsStore.readSpec("Never generated"));
    }

    /** A package created outside of the AI workflow simply has no instructions file. */
    @Test
    public void readingWithoutInstructionsFileReturnsEmpty() throws Exception {
        assertEquals(Optional.empty(), instructionsStore.readSpec("Login flow"));
    }

    /** A malformed file is a real error: it is the only record of what the test cases were asked to cover. */
    @Test
    public void readingAMalformedInstructionsFileFails() throws Exception {
        writeInstructions("{ not json");

        assertThrows(IOException.class, () -> instructionsStore.readSpec("Login flow"));
    }

    @Test
    public void reportsTheInstructionsPath() {
        assertEquals(apRoot.toPath().resolve("specs").resolve("instructions.json"), instructionsStore.instructionsPath());
        assertEquals("specs/instructions.json", instructionsStore.relativeInstructionsPath());
    }

    @Test
    public void rejectsBlankNames() {
        assertThrows(IllegalArgumentException.class, () -> instructionsStore.readSpec(null));
        assertThrows(IllegalArgumentException.class, () -> instructionsStore.readSpec("   "));
    }

    private void writeInstructions(String content) throws IOException {
        Path path = apRoot.toPath().resolve("specs").resolve("instructions.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
