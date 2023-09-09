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
package step.functions.packages.yaml;

import org.junit.Test;
import step.functions.Function;
import step.functions.packages.yaml.model.AutomationPackageKeyword;
import step.functions.packages.yaml.model.AutomationPackageReadingException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AutomationPackageKeywordsExtractorTest {

    private final AutomationPackageKeywordsExtractor extractor = new AutomationPackageKeywordsExtractor();

    @Test
    public void testFunctionExtraction() throws AutomationPackageReadingException, IOException {
        File automationPackageJar = new File("src/test/resources/step/functions/packages/yaml/testpack.jar");
        List<AutomationPackageKeyword> keywords = extractor.extractKeywordsFromAutomationPackage(automationPackageJar);
        assertEquals(1, keywords.size());
    }

}