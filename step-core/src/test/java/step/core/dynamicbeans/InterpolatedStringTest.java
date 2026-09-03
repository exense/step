/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.dynamicbeans;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.core.dynamicbeans.InterpolatedString.Segment;

/**
 * Tests of the parsing of interpolated strings. The expressions aren't evaluated here, they are rendered
 * by {@link #render(String)} using a fixed set of values, which keeps these tests free of any groovy dependency
 */
public class InterpolatedStringTest {

    private static final Map<String, String> VALUES = Map.of("name", "John", "item", "book");

    @Test
    public void testNoPlaceholder() {
        InterpolatedString parsed = InterpolatedString.parse("Hello world");
        Assert.assertFalse(parsed.containsExpressions());
        Assert.assertEquals("Hello world", render("Hello world"));
    }

    @Test
    public void testEmptyString() {
        InterpolatedString parsed = InterpolatedString.parse("");
        Assert.assertTrue(parsed.getSegments().isEmpty());
    }

    @Test
    public void testSinglePlaceholder() {
        Assert.assertEquals("John", render("${name}"));
        Assert.assertTrue(InterpolatedString.parse("${name}").containsExpressions());
    }

    @Test
    public void testPlaceholderPositions() {
        Assert.assertEquals("Hello John", render("Hello ${name}"));
        Assert.assertEquals("John says hi", render("${name} says hi"));
        Assert.assertEquals("a John b", render("a ${name} b"));
    }

    @Test
    public void testAdjacentPlaceholders() {
        Assert.assertEquals("Johnbook", render("${name}${item}"));
        Assert.assertEquals("aJohnbbookc", render("a${name}b${item}c"));
    }

    @Test
    public void testSegments() {
        List<Segment> segments = InterpolatedString.parse("a${name}b").getSegments();
        Assert.assertEquals(3, segments.size());
        Assert.assertFalse(segments.get(0).isExpression());
        Assert.assertEquals("a", segments.get(0).getText());
        Assert.assertTrue(segments.get(1).isExpression());
        Assert.assertEquals("name", segments.get(1).getText());
        Assert.assertEquals(1, segments.get(1).getOffset());
        Assert.assertFalse(segments.get(2).isExpression());
        Assert.assertEquals("b", segments.get(2).getText());
    }

    // Escaping

    @Test
    public void testEscapedPlaceholder() {
        // $${name} renders the literal ${name}: no expression, but the escape sequence still had to be resolved,
        // so the rendered text differs from the source
        Assert.assertEquals("${name}", render("$${name}"));
        Assert.assertFalse(InterpolatedString.parse("$${name}").containsExpressions());
        Assert.assertNotEquals("$${name}", render("$${name}"));
    }

    /**
     * Only the sequence $${ is an escape, so every leading dollar beyond it stays literal. One dollar in, one
     * dollar out - no counting
     */
    @Test
    public void testDollarsInFrontOfAnEscapeStayLiteral() {
        Assert.assertEquals("${name}", render("$${name}"));
        Assert.assertEquals("$${name}", render("$$${name}"));
        Assert.assertEquals("$$${name}", render("$$$${name}"));
        Assert.assertEquals("$$$${name}", render("$$$$${name}"));
    }

    /**
     * A value which contains no ${ is never altered. This is what lets the migration leave the overwhelming
     * majority of the existing values untouched
     */
    @Test
    public void testDoubledDollarIsOnlyAnEscapeInFrontOfABrace() {
        Assert.assertEquals("100$$", render("100$$"));
        Assert.assertEquals("a$$b", render("a$$b"));
        Assert.assertEquals("$$", render("$$"));
    }

    /**
     * The placeholder syntax can't express a literal $ directly in front of an expression, the expression itself
     * is used for that
     */
    @Test
    public void testLiteralDollarFollowedByPlaceholder() {
        List<Segment> segments = InterpolatedString.parse("${'$'}${name}").getSegments();
        Assert.assertEquals(2, segments.size());
        Assert.assertEquals("'$'", segments.get(0).getText());
        Assert.assertEquals("name", segments.get(1).getText());
    }

    @Test
    public void testLoneDollarIsLiteral() {
        Assert.assertEquals("Price: $5", render("Price: $5"));
        Assert.assertEquals("Cost $10 for John", render("Cost $10 for ${name}"));
        Assert.assertEquals("trailing $", render("trailing $"));
    }

    @Test
    public void testUnbracedDollarIsNotInterpolated() {
        // Only the braced form is interpolated, contrary to groovy GStrings
        Assert.assertEquals("$name", render("$name"));
        Assert.assertEquals("John and $name", render("${name} and $name"));
    }

    // Characters which the groovy lexer would otherwise interpret

    @Test
    public void testBackslashesArePreserved() {
        Assert.assertEquals("C:\\temp\\John", render("C:\\temp\\${name}"));
        Assert.assertEquals("\\d+ John", render("\\d+ ${name}"));
        Assert.assertEquals("ends with a backslash \\", render("ends with a backslash \\"));
        Assert.assertEquals("\\n is not a line break for John", render("\\n is not a line break for ${name}"));
    }

    @Test
    public void testDoubleQuotesArePreserved() {
        Assert.assertEquals("{\"a\":\"John\"}", render("{\"a\":\"${name}\"}"));
        Assert.assertEquals("He said \"hi\" to John", render("He said \"hi\" to ${name}"));
    }

    @Test
    public void testMultilineValue() {
        Assert.assertEquals("line1\nline2 John", render("line1\nline2 ${name}"));
        Assert.assertEquals("line1\r\nJohn", render("line1\r\n${name}"));
    }

    // Expression delimiting

    /**
     * The expression ends at the first closing brace. Anything needing a brace of its own belongs in the
     * expression mode of the field
     */
    @Test
    public void testExpressionEndsAtTheFirstClosingBrace() {
        Assert.assertEquals(" a.b.c ", expression("${ a.b.c }"));
        Assert.assertEquals(" list.size() ", expression("${ list.size() }"));
        Assert.assertEquals(" map['k'] ", expression("${ map['k'] }"));
        Assert.assertEquals(" a ? b : c ", expression("${ a ? b : c }"));
        // Text following the expression is literal, even when it contains further braces
        Assert.assertEquals("John} tail", render("${name}} tail"));
    }

    // Error cases

    @Test
    public void testUnterminatedExpression() {
        StringInterpolationException e = Assert.assertThrows(StringInterpolationException.class,
            () -> InterpolatedString.parse("unbalanced ${name"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("no '}' found"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("$${"));
    }

    /**
     * A closure or a map literal would otherwise be silently truncated at its first brace and reach groovy as a
     * broken snippet. The error names the restriction and the way out instead
     */
    @Test
    public void testExpressionContainingABraceIsRejected() {
        StringInterpolationException e = Assert.assertThrows(StringInterpolationException.class,
            () -> InterpolatedString.parse("${ items.collect{ it.id } }"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("contains a brace"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("expression mode"));

        Assert.assertThrows(StringInterpolationException.class, () -> InterpolatedString.parse("${ {-> 'x'}() }"));
        // Brackets are not braces: a list or a map access remains supported
        Assert.assertEquals(" [1,2].sum() ", expression("${ [1,2].sum() }"));
        Assert.assertEquals(" map['k'] ", expression("${ map['k'] }"));
    }

    @Test
    public void testEmptyExpression() {
        StringInterpolationException e = Assert.assertThrows(StringInterpolationException.class,
            () -> InterpolatedString.parse("empty ${}"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("Empty expression"));
        Assert.assertThrows(StringInterpolationException.class, () -> InterpolatedString.parse("${   }"));
    }

    @Test
    public void testNullSource() {
        Assert.assertThrows(IllegalArgumentException.class, () -> InterpolatedString.parse(null));
    }

    // Escaping of pre-existing literals (used by the migrations)

    @Test
    public void testEscapeOnlyTouchesTheExpressionPrefix() {
        Assert.assertEquals("a$${b}", InterpolatedString.escape("a${b}"));
        Assert.assertEquals("$${a}and$${b}", InterpolatedString.escape("${a}and${b}"));
        // A value which contains no ${ is never rewritten by the migration
        Assert.assertEquals("a$$b", InterpolatedString.escape("a$$b"));
        Assert.assertEquals("Price: $5", InterpolatedString.escape("Price: $5"));
        Assert.assertEquals("Hello $name", InterpolatedString.escape("Hello $name"));
        Assert.assertEquals("no dollar at all", InterpolatedString.escape("no dollar at all"));
        Assert.assertEquals("trailing $", InterpolatedString.escape("trailing $"));
        Assert.assertNull(InterpolatedString.escape(null));
    }

    /**
     * The migration only ever has to rewrite the values containing ${, which is what makes its diff small and
     * its effect easy to state
     */
    @Test
    public void testValuesWithoutTheExpressionPrefixAreNeverRewritten() {
        for (String untouched : List.of("plain text", "a$$b", "100$$", "$", "$$", "Price: $5", "Hello $name",
            "C:\\logs\\file.txt", "{\"a\":\"b\"}", "50% $$ done")) {
            Assert.assertSame("Should have been returned as is: " + untouched,
                untouched, InterpolatedString.escape(untouched));
        }
    }

    /**
     * The essential property of the migration: whatever the value was, escaping it and interpolating the result
     * gives the original value back
     */
    @Test
    public void testEscapedLiteralsAreInterpolatedBackToThemselves() {
        List<String> literals = List.of(
            "no placeholder",
            "${name}",
            "Hello ${name} and ${item}",
            "$${name}",
            "$$$${name}",
            "a$$b",
            "$$",
            "$",
            "Price: $5",
            "Hello $name",
            "C:\\logs\\${date}.txt",
            "{\"user\":\"${name}\"}",
            "line1\nline2 ${name}",
            "${JOB_NAME}-$$-${BUILD_ID}",
            "unbalanced ${name",
            "empty ${}",
            "100%$$${x}",
            // Values which the parser would reject if they were not escaped first
            "${ items.collect{ it.id } }",
            "${ [a:1] }",
            "trailing brace ${a}} and }${b}");
        for (String literal : literals) {
            String escaped = InterpolatedString.escape(literal);
            Assert.assertEquals("Round trip failed for <" + literal + "> escaped as <" + escaped + ">",
                literal, renderLiteralsOnly(escaped));
        }
    }

    /**
     * Renders a parsed string that is expected to contain no expression at all, which is what escaping guarantees
     */
    private static String renderLiteralsOnly(String source) {
        StringBuilder result = new StringBuilder();
        for (Segment segment : InterpolatedString.parse(source).getSegments()) {
            Assert.assertFalse("Unexpected expression in escaped value: " + source, segment.isExpression());
            result.append(segment.getText());
        }
        return result.toString();
    }

    // Caching

    @Test
    public void testParsedStringsAreCached() {
        String source = "cached ${name} " + System.nanoTime();
        Assert.assertSame(InterpolatedString.parse(source), InterpolatedString.parse(source));
    }

    /**
     * Renders the parsed string by replacing the expressions with the values of {@link #VALUES}
     */
    private static String render(String source) {
        StringBuilder result = new StringBuilder();
        for (Segment segment : InterpolatedString.parse(source).getSegments()) {
            if (segment.isExpression()) {
                result.append(VALUES.getOrDefault(segment.getText().trim(), "<expression>"));
            } else {
                result.append(segment.getText());
            }
        }
        return result.toString();
    }

    /**
     * @return the text of the single expression of the provided string
     */
    private static String expression(String source) {
        List<Segment> segments = InterpolatedString.parse(source).getSegments();
        Assert.assertEquals("Expected a single segment in '" + source + "'", 1, segments.size());
        Assert.assertTrue(segments.get(0).isExpression());
        return segments.get(0).getText();
    }
}
