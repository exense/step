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
import step.functions.Function;
import step.plugins.jmeter.JMeterFunction;
import step.plugins.jmeter.automation.JMeterFunctionTestplanConversionRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

public class AutomationPackageKeywordsExtractorTest {

    private final AutomationPackageKeywordsExtractor extractor = new AutomationPackageKeywordsExtractor();

    @Test
    public void testExtractionFromPackage() throws AutomationPackageReadingException {
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

    @Test
    public void jmeterKeywordExtractionTest() throws AutomationPackageReadingException {
        File descriptor = new File("src/test/resources/step/functions/packages/yaml/descriptors/jmeterKeywordDescriptor.yml");
        try (InputStream is = new FileInputStream(descriptor)) {
            List<AutomationPackageKeyword> keywords = extractor.extractKeywordsFromDescriptor(is);
            assertEquals(1, keywords.size());
            AutomationPackageKeyword jmeterKeyword = keywords.get(0);
            Function k = jmeterKeyword.getDraftKeyword();
            assertEquals("JMeter keyword from automation package", k.getAttribute("name"));
            assertEquals("JMeter keyword 1", k.getDescription());
            assertFalse(k.isExecuteLocally());
            assertTrue(k.isUseCustomTemplate());
            assertTrue(k.isManaged());
            assertEquals((Integer) 1000, k.getCallTimeout().get());
            assertNotNull("Person", k.getSchema().getJsonString("title").getString());

            assertEquals("jmeterProject1/jmeterProject1.xml", jmeterKeyword.getSpecialAttributes().get(JMeterFunctionTestplanConversionRule.JMETER_TESTPLAN_ATTR));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void emptyKeywordExtractionTest() throws AutomationPackageReadingException {
        File descriptor = new File("src/test/resources/step/functions/packages/yaml/descriptors/emptyKeywordDescriptor.yml");
        try (InputStream is = new FileInputStream(descriptor)) {
            List<AutomationPackageKeyword> keywords = extractor.extractKeywordsFromDescriptor(is);
            assertEquals(0, keywords.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}