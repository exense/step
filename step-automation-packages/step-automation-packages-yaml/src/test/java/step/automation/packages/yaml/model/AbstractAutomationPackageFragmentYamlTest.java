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
package step.automation.packages.yaml.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * The net under the mappings that write an automation package fragment back to disk: an
 * {@code apResource:local:} reference belongs to the editor's memory, and a property no mapping
 * covers would otherwise put one into a file the user owns and commits.
 * <p>
 * Reaching it is a bug of ours - reading is generic, writing is per model - so it repairs and warns
 * rather than refusing the write, which would leave the user unable to save at all.
 */
public class AbstractAutomationPackageFragmentYamlTest {

    private static final Path FRAGMENT = Path.of("my-ap", "keywords.yml");

    private static String repaired(String yaml) {
        return AbstractAutomationPackageFragmentYaml.withoutEditorInternalReferences(yaml, FRAGMENT);
    }

    /**
     * Every mapping being in place is the normal case, so it has to cost nothing and change nothing.
     */
    @Test
    public void aFragmentHoldingNoEditorReferenceIsUntouched() {
        String yaml = "keywords:\n  - General:\n      scriptFile: \"javascript/Kw.js\"\n";

        assertSame(yaml, repaired(yaml));
    }

    /**
     * What is written is what the missing mapping would have produced: the prefix is the only
     * difference between the in-memory form and the descriptor form.
     */
    @Test
    public void theReferenceOfAKeywordIsWrittenAsThePathItWasAuthoredWith() {
        assertEquals("keywords:\n  - General:\n      scriptFile: \"javascript/Kw.js\"\n",
            repaired("keywords:\n  - General:\n      scriptFile: \"apResource:local:javascript/Kw.js\"\n"));
    }

    @Test
    public void everyOccurrenceOfTheFragmentIsRepaired() {
        String repaired = repaired("keywords:\n"
            + "  - General:\n"
            + "      scriptFile: \"apResource:local:javascript/Kw.js\"\n"
            + "      librariesFile: \"apResource:local:lib/fakeLib.jar\"\n"
            + "plans:\n"
            + "  - root:\n"
            + "      dataSet:\n"
            + "        dataSource:\n"
            + "          excel:\n"
            + "            file: \"apResource:local:data/pool.xlsx\"\n");

        assertEquals("keywords:\n"
            + "  - General:\n"
            + "      scriptFile: \"javascript/Kw.js\"\n"
            + "      librariesFile: \"lib/fakeLib.jar\"\n"
            + "plans:\n"
            + "  - root:\n"
            + "      dataSet:\n"
            + "        dataSource:\n"
            + "          excel:\n"
            + "            file: \"data/pool.xlsx\"\n", repaired);
    }

    /**
     * The prefix is removed and nothing else - a relative path may hold a colon of its own, and it is
     * part of the path rather than a separator of the reference.
     */
    @Test
    public void aPathHoldingASeparatorSurvives() {
        assertEquals("file: \"data/2024:Q1/pool.xlsx\"\n",
            repaired("file: \"apResource:local:data/2024:Q1/pool.xlsx\"\n"));
    }

    /**
     * Removing characters from inside a scalar cannot break the document, and the fragment is written
     * without being parsed again - so this is worth pinning rather than assuming.
     */
    @Test
    public void theRepairedFragmentIsStillValidYaml() throws IOException {
        JsonNode fragment = new ObjectMapper(new YAMLFactory()).readTree(
            repaired("keywords:\n  - General:\n      scriptFile: \"apResource:local:javascript/Kw.js\"\n"));

        assertEquals("javascript/Kw.js",
            fragment.path("keywords").path(0).path("General").path("scriptFile").asText());
    }
}
