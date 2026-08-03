package step.ide.ai;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SpecMarkdownParserTest {

    private final SpecMarkdownParser parser = new SpecMarkdownParser();

    @Test
    public void splitsOnLevelTwoHeadings() {
        List<SpecMarkdownParser.ParsedSpec> specs = parser.parse("""
            ## Login with valid credentials
            Open the login page.
            Enter valid credentials.

            ## Login with invalid credentials
            Enter a wrong password.
            """);

        assertEquals(2, specs.size());
        assertEquals("Login with valid credentials", specs.get(0).name());
        assertEquals("Open the login page.\nEnter valid credentials.", specs.get(0).spec());
        assertEquals("Login with invalid credentials", specs.get(1).name());
        assertEquals("Enter a wrong password.", specs.get(1).spec());
    }

    @Test
    public void keepsDeeperHeadingsInsideTheSpec() {
        List<SpecMarkdownParser.ParsedSpec> specs = parser.parse("""
            ## Checkout
            ### Preconditions
            The cart contains one item.
            """);

        assertEquals(1, specs.size());
        assertEquals("Checkout", specs.get(0).name());
        assertEquals("### Preconditions\nThe cart contains one item.", specs.get(0).spec());
    }

    @Test
    public void ignoresContentBeforeTheFirstHeading() {
        List<SpecMarkdownParser.ParsedSpec> specs = parser.parse("""
            # My test suite
            Some introduction.

            ## Only test case
            Do something.
            """);

        assertEquals(1, specs.size());
        assertEquals("Only test case", specs.get(0).name());
        assertEquals("Do something.", specs.get(0).spec());
    }

    @Test
    public void supportsATestCaseWithoutBody() {
        List<SpecMarkdownParser.ParsedSpec> specs = parser.parse("## Empty one\n");

        assertEquals(1, specs.size());
        assertEquals("Empty one", specs.get(0).name());
        assertEquals("", specs.get(0).spec());
    }

    @Test
    public void supportsNamesContainingCommasAndSlashes() {
        List<SpecMarkdownParser.ParsedSpec> specs = parser.parse("## A, B / C\nDo something.\n");

        assertEquals("A, B / C", specs.get(0).name());
    }

    @Test
    public void rejectsADocumentWithoutHeading() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> parser.parse("Just some text without any heading"));

        assertEquals(true, e.getMessage().contains("##"));
    }

    @Test
    public void rejectsAnEmptyDocument() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("   "));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }

    @Test
    public void rejectsAHeadingWithoutName() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("## \nSome spec\n"));
    }
}
