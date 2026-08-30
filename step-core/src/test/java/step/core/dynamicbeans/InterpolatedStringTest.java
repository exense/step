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
        Assert.assertTrue(parsed.isVerbatim());
        Assert.assertFalse(parsed.containsExpressions());
        Assert.assertEquals("Hello world", render("Hello world"));
    }

    @Test
    public void testEmptyString() {
        InterpolatedString parsed = InterpolatedString.parse("");
        Assert.assertTrue(parsed.isVerbatim());
        Assert.assertTrue(parsed.getSegments().isEmpty());
    }

    @Test
    public void testSinglePlaceholder() {
        Assert.assertEquals("John", render("${name}"));
        Assert.assertTrue(InterpolatedString.parse("${name}").containsExpressions());
        Assert.assertFalse(InterpolatedString.parse("${name}").isVerbatim());
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
        // $${name} renders the literal ${name}
        Assert.assertEquals("${name}", render("$${name}"));
        Assert.assertFalse(InterpolatedString.parse("$${name}").containsExpressions());
        // ... but it is not verbatim: the escape sequence had to be resolved
        Assert.assertFalse(InterpolatedString.parse("$${name}").isVerbatim());
    }

    @Test
    public void testLiteralDollarFollowedByPlaceholder() {
        // $$${name} renders a literal $ followed by the value of the expression
        Assert.assertEquals("$John", render("$$${name}"));
    }

    @Test
    public void testEscapedEscape() {
        // $$$${name} renders the literal $${name}
        Assert.assertEquals("$${name}", render("$$$${name}"));
        // and one more level: 5 dollars are 2 escapes followed by a placeholder
        Assert.assertEquals("$$John", render("$$$$${name}"));
    }

    @Test
    public void testEscapeAppliesEverywhere() {
        Assert.assertEquals("100$", render("100$$"));
        Assert.assertEquals("a$b", render("a$$b"));
        Assert.assertEquals("$", render("$$"));
    }

    @Test
    public void testLoneDollarIsLiteral() {
        Assert.assertEquals("Price: $5", render("Price: $5"));
        Assert.assertTrue(InterpolatedString.parse("Price: $5").isVerbatim());
        Assert.assertEquals("Cost $10 for John", render("Cost $10 for ${name}"));
        Assert.assertEquals("trailing $", render("trailing $"));
    }

    @Test
    public void testUnbracedDollarIsNotInterpolated() {
        // Only the braced form is interpolated, contrary to groovy GStrings
        Assert.assertEquals("$name", render("$name"));
        Assert.assertTrue(InterpolatedString.parse("$name").isVerbatim());
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

    @Test
    public void testNestedBracesInExpression() {
        Assert.assertEquals(" [1,2].collect{ it } ", expression("${ [1,2].collect{ it } }"));
        Assert.assertEquals("a.b{c{d}}e", expression("${a.b{c{d}}e}"));
    }

    @Test
    public void testBracesInStringLiteralsOfExpression() {
        Assert.assertEquals(" map['}'] ", expression("${ map['}'] }"));
        Assert.assertEquals(" \"}\" ", expression("${ \"}\" }"));
        Assert.assertEquals(" '${' ", expression("${ '${' }"));
    }

    @Test
    public void testNestedGStringInExpression() {
        Assert.assertEquals(" \"a${b}c\" ", expression("${ \"a${b}c\" }"));
    }

    @Test
    public void testTripleQuotedStringInExpression() {
        Assert.assertEquals(" \"\"\"}\"\"\" ", expression("${ \"\"\"}\"\"\" }"));
        Assert.assertEquals(" '''}''' ", expression("${ '''}''' }"));
    }

    @Test
    public void testEscapedQuoteInStringLiteralOfExpression() {
        Assert.assertEquals(" 'it\\'s }' ", expression("${ 'it\\'s }' }"));
    }

    // Error cases

    @Test
    public void testUnterminatedExpression() {
        StringInterpolationException e = Assert.assertThrows(StringInterpolationException.class,
            () -> InterpolatedString.parse("unbalanced ${name"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("no matching '}'"));
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("$${"));
    }

    @Test
    public void testUnterminatedNestedBrace() {
        Assert.assertThrows(StringInterpolationException.class, () -> InterpolatedString.parse("${ a{b }"));
    }

    @Test
    public void testUnterminatedStringLiteralInExpression() {
        Assert.assertThrows(StringInterpolationException.class, () -> InterpolatedString.parse("${ 'abc }"));
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
    public void testEscapeOnlyTouchesSignificantDollars() {
        Assert.assertEquals("a$${b}", InterpolatedString.escape("a${b}"));
        Assert.assertEquals("a$$$b", InterpolatedString.escape("a$$b"));
        // Nothing became significant here, the value is returned as is
        Assert.assertEquals("Price: $5", InterpolatedString.escape("Price: $5"));
        Assert.assertEquals("Hello $name", InterpolatedString.escape("Hello $name"));
        Assert.assertEquals("no dollar at all", InterpolatedString.escape("no dollar at all"));
        Assert.assertEquals("trailing $", InterpolatedString.escape("trailing $"));
        Assert.assertNull(InterpolatedString.escape(null));
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
            "100%$$${x}");
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
