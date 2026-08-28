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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * A string literal parsed into its literal and expression segments in order to support the interpolation
 * of expressions within plain (non dynamic) string values.
 * <p>
 * The syntax is intentionally kept minimal and is <b>not</b> the Groovy GString syntax. Only the following
 * sequences are interpreted, everything else (in particular backslashes and quotes) is preserved verbatim:
 * <ul>
 *     <li><code>${...}</code> delimits an expression. The content is passed as is to the expression handler.
 *     The closing brace is located by balancing braces while skipping over Groovy string literals, so that
 *     expressions such as <code>${ map['}'] }</code> or <code>${ list.collect{ it } }</code> are supported.</li>
 *     <li><code>$$</code> is the escape sequence for a single literal <code>$</code>. It applies everywhere,
 *     not only in front of a brace, which makes the escaping complete: <code>$${name}</code> renders the
 *     literal <code>${name}</code>, <code>$$${name}</code> renders a <code>$</code> followed by the value of
 *     the expression <code>name</code>, and <code>$$$${name}</code> renders the literal <code>$${name}</code>.</li>
 *     <li>A <code>$</code> which is followed neither by a <code>$</code> nor by a <code>{</code> is a literal
 *     <code>$</code>. In particular <code>$name</code> is <b>not</b> interpolated, only the braced form is.</li>
 * </ul>
 * Instances are immutable and cached, as the same literals are typically re-parsed for every execution of
 * every artefact instance.
 */
public class InterpolatedString {

    private static final int PARSE_CACHE_SIZE = 1000;

    private static final Cache<String, InterpolatedString> parseCache = CacheBuilder.newBuilder().maximumSize(PARSE_CACHE_SIZE).build();

    /**
     * A part of a parsed string: either a piece of literal text or an expression to be evaluated
     */
    public static class Segment {

        private final boolean expression;
        private final String text;
        private final int offset;

        private Segment(boolean expression, String text, int offset) {
            this.expression = expression;
            this.text = text;
            this.offset = offset;
        }

        /**
         * @return true if the text of this segment is an expression to be evaluated, false if it is literal text
         */
        public boolean isExpression() {
            return expression;
        }

        /**
         * @return the expression to be evaluated (without the enclosing <code>${</code> and <code>}</code>) or
         * the literal text, with all escape sequences already resolved
         */
        public String getText() {
            return text;
        }

        /**
         * @return the position of this segment within the parsed string, used for error reporting
         */
        public int getOffset() {
            return offset;
        }
    }

    private final String source;
    private final List<Segment> segments;
    private final boolean containsExpressions;
    private final boolean verbatim;

    private InterpolatedString(String source, List<Segment> segments) {
        this.source = source;
        this.segments = Collections.unmodifiableList(segments);
        this.containsExpressions = segments.stream().anyMatch(Segment::isExpression);
        this.verbatim = !containsExpressions &&
            (segments.isEmpty() ? source.isEmpty() : segments.size() == 1 && segments.get(0).getText().equals(source));
    }

    /**
     * Parses the provided string into its literal and expression segments
     *
     * @param source the string to be parsed
     * @return the parsed string
     * @throws StringInterpolationException if the string contains a malformed expression
     */
    public static InterpolatedString parse(String source) {
        if (source == null) {
            throw new IllegalArgumentException("The string to be parsed must not be null");
        }
        InterpolatedString cached = parseCache.getIfPresent(source);
        if (cached != null) {
            return cached;
        }
        // Malformed strings aren't cached. They are rare and are re-parsed on every evaluation
        InterpolatedString parsed = doParse(source);
        parseCache.put(source, parsed);
        return parsed;
    }

    public String getSource() {
        return source;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    /**
     * @return true if the string contains at least one expression to be evaluated
     */
    public boolean containsExpressions() {
        return containsExpressions;
    }

    /**
     * @return true if the string contains neither an expression nor an escape sequence and is therefore to be
     * used as is. This is the case for the vast majority of the plain values
     */
    public boolean isVerbatim() {
        return verbatim;
    }

    private static InterpolatedString doParse(String source) {
        List<Segment> segments = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int literalOffset = 0;
        int length = source.length();
        int i = 0;
        while (i < length) {
            char c = source.charAt(i);
            if (c == '$' && i + 1 < length) {
                char next = source.charAt(i + 1);
                if (next == '$') {
                    // Escape sequence: emit a single literal $
                    literal.append('$');
                    i += 2;
                    continue;
                }
                if (next == '{') {
                    int expressionStart = i + 2;
                    int expressionEnd = findClosingBrace(source, expressionStart);
                    if (expressionEnd < 0) {
                        throw new StringInterpolationException("Unterminated expression in '" + source + "': no matching '}' found for the '${' at position " + i + ". Use '$${' if a literal '${' was intended.");
                    }
                    String expression = source.substring(expressionStart, expressionEnd);
                    if (expression.trim().isEmpty()) {
                        throw new StringInterpolationException("Empty expression at position " + i + " in '" + source + "'. Use '$${}' if a literal '${}' was intended.");
                    }
                    if (literal.length() > 0) {
                        segments.add(new Segment(false, literal.toString(), literalOffset));
                        literal.setLength(0);
                    }
                    segments.add(new Segment(true, expression, i));
                    i = expressionEnd + 1;
                    literalOffset = i;
                    continue;
                }
            }
            literal.append(c);
            i++;
        }
        if (literal.length() > 0) {
            segments.add(new Segment(false, literal.toString(), literalOffset));
        }
        return new InterpolatedString(source, segments);
    }

    /**
     * Searches the closing brace matching the opening brace of an expression, skipping over any brace contained
     * in a Groovy string literal
     *
     * @param source the string being parsed
     * @param from   the position of the first character following the opening brace
     * @return the position of the matching closing brace or -1 if there is none
     */
    private static int findClosingBrace(String source, int from) {
        int length = source.length();
        int depth = 1;
        int i = from;
        while (i < length) {
            char c = source.charAt(i);
            if (c == '\'' || c == '"') {
                i = skipStringLiteral(source, i);
                if (i < 0) {
                    return -1;
                }
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    /**
     * Skips over a Groovy string literal, supporting single and triple quoted literals, backslash escapes and
     * the placeholders nested in double quoted GStrings
     *
     * @param source the string being parsed
     * @param start  the position of the opening quote
     * @return the position of the first character following the string literal or -1 if it is unterminated
     */
    private static int skipStringLiteral(String source, int start) {
        int length = source.length();
        char quote = source.charAt(start);
        boolean triple = isTripleQuoteAt(source, start, quote);
        int i = start + (triple ? 3 : 1);
        while (i < length) {
            char c = source.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (quote == '"' && c == '$' && i + 1 < length && source.charAt(i + 1) == '{') {
                int nestedEnd = findClosingBrace(source, i + 2);
                if (nestedEnd < 0) {
                    return -1;
                }
                i = nestedEnd + 1;
                continue;
            }
            if (c == quote) {
                if (!triple) {
                    return i + 1;
                }
                if (isTripleQuoteAt(source, i, quote)) {
                    return i + 3;
                }
            }
            i++;
        }
        return -1;
    }

    private static boolean isTripleQuoteAt(String source, int position, char quote) {
        return position + 2 < source.length() && source.charAt(position + 1) == quote && source.charAt(position + 2) == quote;
    }
}
