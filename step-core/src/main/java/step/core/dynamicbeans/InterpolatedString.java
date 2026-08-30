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
 *     <li><code>${...}</code> delimits an expression, ended by the <b>first</b> <code>}</code>. The content is
 *     passed as is to the expression handler. An expression can therefore not contain a brace, which rules out
 *     closures and map literals - those belong in the expression mode of the field, which stays available for
 *     anything the placeholder syntax can't express.</li>
 *     <li><code>$${</code> is the escape sequence for a literal <code>${</code>, following the convention used
 *     by Apache Commons Text and docker compose. Nothing else is an escape: a value which doesn't contain
 *     <code>${</code> is never altered, so <code>a$$b</code>, <code>100$$</code> and <code>Price: $5</code> are
 *     all preserved verbatim.</li>
 *     <li>A <code>$</code> which doesn't open or escape an expression is a literal <code>$</code>. In particular
 *     <code>$name</code> is <b>not</b> interpolated, only the braced form is.</li>
 * </ul>
 * A literal <code>$</code> immediately followed by an expression is written <code>${'$'}${name}</code>, the
 * placeholder syntax having no way to express it directly.
 * <p>
 * Instances are immutable and cached, as the same literals are typically re-parsed for every execution of
 * every artefact instance.
 */
public class InterpolatedString {

    /**
     * The only sequence which is significant in a plain value: it either opens an expression or, doubled, escapes
     * one. A value which doesn't contain it is always used as is
     */
    public static final String EXPRESSION_PREFIX = "${";
    private static final String ESCAPED_EXPRESSION_PREFIX = "$${";

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

    private InterpolatedString(String source, List<Segment> segments) {
        this.source = source;
        this.segments = Collections.unmodifiableList(segments);
        this.containsExpressions = segments.stream().anyMatch(Segment::isExpression);
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

    /**
     * Escapes the provided literal so that interpolating the result yields the literal back, unchanged.
     * <p>
     * This is the exact inverse of {@link #parse(String)} and is used by the migrations which have to preserve the
     * meaning of the values authored before the interpolation existed. Only {@code ${} is significant, so a value
     * which doesn't contain it is returned unchanged - in particular a value containing {@code $$} or a lone
     * {@code $} is never rewritten:
     * <ul>
     *     <li>{@code a${b}} becomes {@code a$${b}}</li>
     *     <li>{@code a$$b}, {@code 100$$} and {@code Price: $5} are returned unchanged</li>
     * </ul>
     * Note that the operation is <b>not</b> idempotent: escaping an already escaped value escapes it twice. The
     * migrations using it are therefore gated on the version of the migrated document.
     *
     * @param literal the literal to escape, may be null
     * @return the escaped literal
     */
    public static String escape(String literal) {
        if (literal == null) {
            return null;
        }
        return literal.replace(EXPRESSION_PREFIX, ESCAPED_EXPRESSION_PREFIX);
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


    private static InterpolatedString doParse(String source) {
        List<Segment> segments = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int literalOffset = 0;
        int length = source.length();
        int i = 0;
        while (i < length) {
            char c = source.charAt(i);
            if (c == '$' && i + 2 < length && source.charAt(i + 1) == '$' && source.charAt(i + 2) == '{') {
                // Escape sequence: emit a literal ${
                literal.append(EXPRESSION_PREFIX);
                i += 3;
                continue;
            }
            if (c == '$' && i + 1 < length && source.charAt(i + 1) == '{') {
                int expressionStart = i + 2;
                int expressionEnd = source.indexOf('}', expressionStart);
                if (expressionEnd < 0) {
                    throw new StringInterpolationException("Unterminated expression in '" + source + "': no '}' found after the '${' at position " + i + ". Use '$${' if a literal '${' was intended.");
                }
                String expression = source.substring(expressionStart, expressionEnd);
                if (expression.trim().isEmpty()) {
                    throw new StringInterpolationException("Empty expression at position " + i + " in '" + source + "'. Use '$${}' if a literal '${}' was intended.");
                }
                if (expression.indexOf('{') >= 0) {
                    throw new StringInterpolationException("The expression '" + expression + "' at position " + i + " in '" + source
                        + "' contains a brace. Expressions embedded in a plain value are delimited by the first '}' and can therefore not contain "
                        + "closures or map literals. Use the expression mode of the field for such expressions.");
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
            literal.append(c);
            i++;
        }
        if (literal.length() > 0) {
            segments.add(new Segment(false, literal.toString(), literalOffset));
        }
        return new InterpolatedString(source, segments);
    }
}
