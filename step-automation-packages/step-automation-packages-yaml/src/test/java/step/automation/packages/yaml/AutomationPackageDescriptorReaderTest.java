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
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.core.scheduler.automation.AutomationPackageSchedule;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;
import step.plans.parser.yaml.YamlPlan;
import step.plugins.jmeter.automation.YamlJMeterFunction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class AutomationPackageDescriptorReaderTest {

    private final AutomationPackageDescriptorReader reader;

    public AutomationPackageDescriptorReaderTest() {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageScheduleRegistration.registerSerialization(serializationRegistry);
        reader = new AutomationPackageDescriptorReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, serializationRegistry);
    }

    @Test
    public void jmeterKeywordReadTest() throws AutomationPackageReadingException {
        File descriptor = new File("src/test/resources/step/automation/packages/yaml/descriptors/jmeterKeywordDescriptor.yml");
        try (InputStream is = new FileInputStream(descriptor)) {
            AutomationPackageDescriptorYaml automationPackage = reader.readAutomationPackageDescriptor(is, "");
            assertNotNull(automationPackage);
            List<YamlAutomationPackageKeyword> keywords = automationPackage.getKeywords();
            assertEquals(1, keywords.size());
            YamlAutomationPackageKeyword jmeterKeyword = keywords.get(0);
            YamlJMeterFunction k = (YamlJMeterFunction) jmeterKeyword.getYamlKeyword();
            assertEquals("JMeter keyword from automation package", k.getName());
            assertEquals("JMeter keyword 1", k.getDescription());
            assertFalse(k.isExecuteLocally());
            assertTrue(k.isUseCustomTemplate());
            assertEquals((Integer) 1000, k.getCallTimeout().get());
            assertNotNull("string", k.getSchema().getJsonObject("properties").getJsonObject("firstName").getJsonString("type"));

            assertEquals("jmeterProject1/jmeterProject1.xml", k.getJmeterTestplan().get());

            assertEquals("valueA", k.getRouting().get("criteriaA"));
            assertEquals("valueB", k.getRouting().get("criteriaB"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void completeDescriptorReadTest() throws AutomationPackageReadingException {
        File file = new File("src/test/resources/step/automation/packages/yaml/descriptors/completeDescriptor.yml");
        try (InputStream is = new FileInputStream(file)) {
            AutomationPackageDescriptorYaml descriptor = reader.readAutomationPackageDescriptor(is, "");
            assertNotNull(descriptor);
            List<YamlAutomationPackageKeyword> keywords = descriptor.getKeywords();
            assertEquals(1, keywords.size());
            YamlAutomationPackageKeyword jmeterKeyword = keywords.get(0);
            YamlJMeterFunction k = (YamlJMeterFunction) jmeterKeyword.getYamlKeyword();
            assertEquals("JMeter keyword from automation package", k.getName());

            // check parsed plans
            List<YamlPlan> plans = descriptor.getPlans();
            assertEquals(2, plans.size());
            assertEquals("First Plan", plans.get(0).getName());
            assertEquals("Second Plan", plans.get(1).getName());

            // check parsed scheduler
            List<AutomationPackageSchedule> schedules = descriptor.getAdditionalField(AutomationPackageSchedule.FIELD_NAME_IN_AP);
            assertEquals(2, schedules.size());
            AutomationPackageSchedule firstTask = schedules.get(0);
            assertEquals("My first task", firstTask.getName());
            assertEquals("First Plan", firstTask.getPlanName());
            assertEquals("*/5 * * * *", firstTask.getCron());
            assertEquals("TEST", firstTask.getExecutionParameters().get("environment"));

            assertEquals(Arrays.asList("importPlans.yml", "importKeywords.yml"), descriptor.getFragments());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void emptyKeywordReadTest() throws AutomationPackageReadingException {
        File file = new File("src/test/resources/step/automation/packages/yaml/descriptors/emptyKeywordDescriptor.yml");
        try (InputStream is = new FileInputStream(file)) {
            AutomationPackageDescriptorYaml descriptor = reader.readAutomationPackageDescriptor(is, "");
            assertNotNull(descriptor);
            List<YamlAutomationPackageKeyword> keywords = descriptor.getKeywords();
            assertEquals(0, keywords.size());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}