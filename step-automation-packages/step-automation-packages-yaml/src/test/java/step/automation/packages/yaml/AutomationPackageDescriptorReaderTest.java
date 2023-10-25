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
import step.automation.packages.model.AutomationPackageKeyword;
import step.automation.packages.model.AutomationPackageSchedulerTask;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.functions.Function;
import step.plans.parser.yaml.model.YamlPlan;
import step.plugins.jmeter.automation.JMeterFunctionTestplanConversionRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class AutomationPackageDescriptorReaderTest {

    private final AutomationPackageDescriptorReader reader = new AutomationPackageDescriptorReader();



    @Test
    public void jmeterKeywordReadTest() throws AutomationPackageReadingException {
        File descriptor = new File("src/test/resources/step/automation/packages/yaml/descriptors/jmeterKeywordDescriptor.yml");
        try (InputStream is = new FileInputStream(descriptor)) {
            AutomationPackageDescriptorYaml automationPackage = reader.readAutomationPackageDescriptor(is);
            assertNotNull(automationPackage);
            List<AutomationPackageKeyword> keywords = automationPackage.getKeywords();
            assertEquals(1, keywords.size());
            AutomationPackageKeyword jmeterKeyword = keywords.get(0);
            Function k = jmeterKeyword.getDraftKeyword();
            assertEquals("JMeter keyword from automation package", k.getAttribute("name"));
            assertEquals("JMeter keyword 1", k.getDescription());
            assertFalse(k.isExecuteLocally());
            assertTrue(k.isUseCustomTemplate());
            assertTrue(k.isManaged());
            assertEquals((Integer) 1000, k.getCallTimeout().get());
            assertNotNull("string", k.getSchema().getJsonObject("properties").getJsonObject("firstName").getJsonString("type"));

            assertEquals("jmeterProject1/jmeterProject1.xml", jmeterKeyword.getSpecialAttributes().get(JMeterFunctionTestplanConversionRule.JMETER_TESTPLAN_ATTR));

            assertEquals("valueA", jmeterKeyword.getDraftKeyword().getTokenSelectionCriteria().get("criteriaA"));
            assertEquals("valueB", jmeterKeyword.getDraftKeyword().getTokenSelectionCriteria().get("criteriaB"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void completeDescriptorReadTest() throws AutomationPackageReadingException {
        File file = new File("src/test/resources/step/automation/packages/yaml/descriptors/completeDescriptor.yml");
        try (InputStream is = new FileInputStream(file)) {
            AutomationPackageDescriptorYaml descriptor = reader.readAutomationPackageDescriptor(is);
            assertNotNull(descriptor);
            List<AutomationPackageKeyword> keywords = descriptor.getKeywords();
            assertEquals(1, keywords.size());
            AutomationPackageKeyword jmeterKeyword = keywords.get(0);
            Function k = jmeterKeyword.getDraftKeyword();
            assertEquals("JMeter keyword from automation package", k.getAttribute("name"));

            // check parsed plans
            List<YamlPlan> plans = descriptor.getPlans();
            assertEquals(2, plans.size());
            assertEquals("First Plan", plans.get(0).getName());
            assertEquals("Second Plan", plans.get(1).getName());

            // check parsed scheduler
            List<AutomationPackageSchedulerTask> scheduler = descriptor.getScheduler();
            assertEquals(2, scheduler.size());
            AutomationPackageSchedulerTask firstTask = scheduler.get(0);
            assertEquals("My first task", firstTask.getName());
            assertEquals("First Plan", firstTask.getPlanName());
            assertEquals("*/5 * * * *", firstTask.getCron());
            assertEquals("TEST", firstTask.getEnvironment());

            assertEquals(Arrays.asList("importPlans.yml", "importKeywords.yml"), descriptor.getFragments());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void emptyKeywordReadTest() throws AutomationPackageReadingException {
        File file = new File("src/test/resources/step/automation/packages/yaml/descriptors/emptyKeywordDescriptor.yml");
        try (InputStream is = new FileInputStream(file)) {
            AutomationPackageDescriptorYaml descriptor = reader.readAutomationPackageDescriptor(is);
            assertNotNull(descriptor);
            List<AutomationPackageKeyword> keywords = descriptor.getKeywords();
            assertEquals(0, keywords.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}