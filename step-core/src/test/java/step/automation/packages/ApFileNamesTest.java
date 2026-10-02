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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * The one convention by which every file an automation package holds is named after its entity - the YAML
 * fragment of a plan, the script of a keyword.
 */
public class ApFileNamesTest {

    @Test
    public void sanitizesTheNameIntoOneUsableSegment() {
        assertEquals("a_b", ApFileNames.sanitize("a/b"));
        assertEquals("a_b", ApFileNames.sanitize("a\\b"));
        assertEquals("a_b_c", ApFileNames.sanitize("a:b?c"));
        assertEquals("My_Keyword", ApFileNames.sanitize("  My   Keyword  "));
        assertEquals("Kw", ApFileNames.sanitize("...Kw..."));
        assertEquals(100, ApFileNames.sanitize("x".repeat(200)).length());
    }

    /**
     * The two things {@code URLEncoder} - the encoding this replaced - leaves as they are, and which
     * Windows then refuses.
     */
    @Test
    public void handlesWhatWindowsRefuses() {
        assertEquals("a_b", ApFileNames.sanitize("a*b"));
        assertEquals("_nul", ApFileNames.sanitize("nul"));
        assertEquals("_CON", ApFileNames.sanitize("CON"));
    }

    /**
     * A name kept as it is rather than percent-encoded: the file is source the user reads in a diff.
     */
    @Test
    public void keepsNonAsciiNamesReadable() {
        assertEquals("Récupération_données", ApFileNames.sanitize("Récupération données"));
    }

    /**
     * Rather than falling back to a name of our own: a file the user cannot connect to the entity they
     * named is worse than a refusal they can act on.
     */
    @Test
    public void refusesANameNothingCanBeDerivedFrom() {
        assertThrows(IllegalArgumentException.class, () -> ApFileNames.sanitize(null));
        assertThrows(IllegalArgumentException.class, () -> ApFileNames.sanitize("  "));
        assertThrows(IllegalArgumentException.class, () -> ApFileNames.sanitize("///"));
    }
}
