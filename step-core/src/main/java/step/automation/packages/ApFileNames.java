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
package step.automation.packages;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Derives the name of a file <b>inside an automation package</b> from the name of the entity it holds:
 * the YAML fragment of a plan, the script of a keyword. One convention for all of them, so that a package
 * does not end up holding {@code plans/My Plan.yml} next to {@code keywords/My_Keyword.groovy}.
 * <p>
 * These files are the user's source, read in diffs and in a file tree, which rules out both a uuid and a
 * reversible encoding: {@code URLEncoder} would turn {@code Récupération données} into
 * {@code R%C3%A9cup%C3%A9ration donn%C3%A9es}, and leaves {@code *} - illegal on Windows - untouched.
 */
public final class ApFileNames {

    /**
     * Characters that are illegal in a file name on Windows, plus the separators - a name is one path
     * segment, never a path.
     */
    private static final Pattern ILLEGAL_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Names Windows refuses whatever the extension.
     */
    private static final Set<String> RESERVED_NAMES = Set.of("CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    private static final int MAX_NAME_LENGTH = 100;

    private ApFileNames() {
    }

    /**
     * Turns an entity name into one usable file name segment: path separators and the characters Windows
     * rejects become {@code _}, whitespace runs become a single {@code _}, leading and trailing dots,
     * spaces and underscores are dropped, and the result is capped at {@value #MAX_NAME_LENGTH}
     * characters. A name that is reserved on Windows ({@code CON}, {@code NUL}, ...) is prefixed.
     * <p>
     * The extension is not part of it - append it to the result, so that a long name is capped without
     * eating the extension.
     *
     * @throws IllegalArgumentException if {@code name} is null or blank, or if nothing usable is left of
     *                                  it. Naming the file after something else - a default, a uuid -
     *                                  would hide from the user that the entity they named is not the one
     *                                  they will find in their package.
     */
    public static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Unable to derive a file name from an empty name");
        }
        String sanitized = ILLEGAL_CHARACTERS.matcher(name).replaceAll("_");
        sanitized = WHITESPACE.matcher(sanitized).replaceAll("_");
        sanitized = trim(sanitized);
        if (sanitized.length() > MAX_NAME_LENGTH) {
            sanitized = trim(sanitized.substring(0, MAX_NAME_LENGTH));
        }
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Unable to derive a file name from the name '" + name
                + "': nothing usable in a file name is left of it");
        }
        if (RESERVED_NAMES.contains(sanitized.toUpperCase(Locale.ROOT))) {
            return "_" + sanitized;
        }
        return sanitized;
    }

    private static String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isTrimmable(value.charAt(start))) {
            start++;
        }
        while (end > start && isTrimmable(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean isTrimmable(char c) {
        return c == '.' || c == '_' || c == ' ';
    }
}
