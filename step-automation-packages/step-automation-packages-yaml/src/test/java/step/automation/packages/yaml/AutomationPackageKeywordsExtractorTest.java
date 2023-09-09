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
package step.automation.packages.yaml;

import org.junit.Test;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.yaml.model.AutomationPackageKeyword;
import step.plugins.jmeter.JMeterFunction;
import step.plugins.jmeter.automation.JMeterFunctionTestplanConversionRule;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AutomationPackageKeywordsExtractorTest {

    private final AutomationPackageKeywordsExtractor extractor = new AutomationPackageKeywordsExtractor();

    @Test
    public void testFunctionExtraction() throws AutomationPackageReadingException {
        File automationPackageJar = new File("src/test/resources/step/functions/packages/yaml/testpack.jar");
        List<AutomationPackageKeyword> keywords = extractor.extractKeywordsFromAutomationPackage(automationPackageJar);
        assertEquals(1, keywords.size());
        AutomationPackageKeyword automationPackageKeyword = keywords.get(0);
        assertEquals(JMeterFunction.class, automationPackageKeyword.getDraftKeyword().getClass());
        assertEquals(
                "jmeterProject1/jmeterProject1.xml",
                automationPackageKeyword.getSpecialAttributes().get(JMeterFunctionTestplanConversionRule.JMETER_TESTPLAN_ATTR)
        );
    }

}