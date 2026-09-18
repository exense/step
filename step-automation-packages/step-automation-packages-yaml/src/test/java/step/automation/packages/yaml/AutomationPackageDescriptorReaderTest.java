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
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.core.plans.Plan;
import step.core.scheduler.automation.AutomationPackageSchedule;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;
import step.core.yaml.YamlMetadata;
import step.parameter.Parameter;
import step.parameter.automation.AutomationPackageParameter;
import step.parameter.automation.AutomationPackageParameterJsonSchema;
import step.plans.parser.yaml.YamlPlan;
import step.plans.parser.yaml.YamlPlanReader;
import step.plugins.jmeter.automation.YamlJMeterFunction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AutomationPackageDescriptorReaderTest {

    private final AutomationPackageDescriptorReader reader;

    public AutomationPackageDescriptorReaderTest() {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageScheduleRegistration.registerSerialization(serializationRegistry);
        serializationRegistry.register(AutomationPackageParameterJsonSchema.FIELD_NAME_IN_AP, AutomationPackageParameter.class);
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

            assertEquals(Arrays.asList("importPlans.yml", "importKeywords.yml"), descriptor.getFragments().stream().map(f -> f.getValue()).collect(Collectors.toUnmodifiableList()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void metadataReadTest() throws AutomationPackageReadingException, IOException {
        File file = new File("src/test/resources/step/automation/packages/yaml/descriptors/metadataDescriptor.yml");
        try (InputStream is = new FileInputStream(file)) {
            AutomationPackageDescriptorYaml descriptor = reader.readAutomationPackageDescriptor(is, "");

            assertEquals(Map.of("owner", "team-a", "tags", List.of("smoke", "nightly")), descriptor.getMetadata());

            YamlJMeterFunction keyword = (YamlJMeterFunction) descriptor.getKeywords().get(0).getYamlKeyword();
            assertEquals(Map.of("origin", Map.of("tool", "generator", "version", 2)), keyword.getMetadata());

            YamlPlan yamlPlan = descriptor.getPlans().get(0);
            Map<String, Object> planMetadata = new HashMap<>();
            planMetadata.put("requirements", List.of(Map.of("id", "REQ-1", "covered", true), "REQ-2"));
            planMetadata.put("empty", null);
            assertEquals(planMetadata, yamlPlan.getMetadata());

            // the metadata is persisted in the custom fields of the plan and written back to yaml
            YamlPlanReader planReader = reader.getPlanReader();
            Plan plan = planReader.yamlPlanToPlan(yamlPlan);
            assertEquals(planMetadata, plan.getCustomField(YamlMetadata.METADATA_FIELD));
            assertEquals(planMetadata, planReader.planToYamlPlan(plan).getMetadata());

            assertEquals(Map.of("owner", "team-b"), descriptor.getPlansPlainText().get(0).getMetadata());

            List<AutomationPackageSchedule> schedules = descriptor.getAdditionalField(AutomationPackageSchedule.FIELD_NAME_IN_AP);
            assertEquals(Map.of("owner", "team-c"), schedules.get(0).getMetadata());

            List<AutomationPackageParameter> parameters = descriptor.getAdditionalField(AutomationPackageParameterJsonSchema.FIELD_NAME_IN_AP);
            AutomationPackageParameter yamlParameter = parameters.get(0);
            assertEquals(Map.of("owner", "team-d"), yamlParameter.getMetadata());
            Parameter parameter = yamlParameter.toParameter();
            assertEquals(Map.of("owner", "team-d"), parameter.getCustomField(YamlMetadata.METADATA_FIELD));
            assertEquals(Map.of("owner", "team-d"), AutomationPackageParameter.fromParameter(parameter).getMetadata());
        }
    }

    @Test
    public void fragmentMetadataReadTest() throws AutomationPackageReadingException, IOException {
        File file = new File("src/test/resources/step/automation/packages/yaml/descriptors/metadataFragment.yml");
        try (InputStream is = new FileInputStream(file)) {
            AutomationPackageFragmentYaml fragment = reader.readAutomationPackageFragment(is, "metadataFragment.yml", "", null);
            // the metadata of a fragment is accepted and kept in the yaml model, but it is not the one of the package
            assertEquals(Map.of("origin", "AI"), fragment.getMetadata());
            assertEquals(1, fragment.getPlans().size());
        }
    }

    @Test
    public void missingMetadataIsNotSerialized() throws AutomationPackageReadingException, IOException {
        File file = new File("src/test/resources/step/automation/packages/yaml/descriptors/completeDescriptor.yml");
        try (InputStream is = new FileInputStream(file)) {
            AutomationPackageDescriptorYaml descriptor = reader.readAutomationPackageDescriptor(is, "");
            YamlPlan yamlPlan = descriptor.getPlans().get(0);
            assertNull(yamlPlan.getMetadata());
            YamlPlanReader planReader = reader.getPlanReader();
            Plan plan = planReader.yamlPlanToPlan(yamlPlan);
            assertNull(plan.getCustomFields());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            planReader.writeYamlPlan(out, plan);
            assertFalse(out.toString(StandardCharsets.UTF_8), out.toString(StandardCharsets.UTF_8).contains(YamlMetadata.METADATA_FIELD));
        }
    }

    @Test
    public void metadataWithInvalidKeyTest() throws IOException {
        assertInvalidDescriptor("metadataWithInvalidKeyDescriptor.yml", "invalid.key");
    }

    @Test
    public void metadataNotAMapTest() throws IOException {
        assertInvalidDescriptor("metadataNotAMapDescriptor.yml", "#/metadata: expected type: JSONObject");
    }

    private void assertInvalidDescriptor(String fileName, String expectedMessagePart) throws IOException {
        File file = new File("src/test/resources/step/automation/packages/yaml/descriptors/" + fileName);
        try (InputStream is = new FileInputStream(file)) {
            AutomationPackageReadingException e = assertThrows(AutomationPackageReadingException.class, () -> reader.readAutomationPackageDescriptor(is, ""));
            assertTrue(e.getMessage(), e.getMessage().contains(expectedMessagePart));
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
