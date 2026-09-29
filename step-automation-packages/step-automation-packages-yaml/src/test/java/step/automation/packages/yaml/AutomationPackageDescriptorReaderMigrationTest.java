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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import step.artefacts.CallPlan;
import step.artefacts.Echo;
import step.artefacts.PerformanceAssert;
import step.artefacts.Sequence;
import step.artefacts.Set;
import step.artefacts.ThreadGroup;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;
import step.plugins.functions.types.automation.YamlCompositeFunction;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The plans of an automation package declaring an older schema version are migrated before the descriptor is
 * validated, since the json schema only describes the current version
 */
public class AutomationPackageDescriptorReaderMigrationTest {

    private static final String BEFORE_AFTER_OLD_VERSION_DESCRIPTOR =
        "src/test/resources/step/automation/packages/yaml/descriptors/beforeAfterOldVersionDescriptor.yml";

    private static final String COMPOSITES_OLD_VERSION =
        "keywords:\n" +
        "  - Composite:\n" +
        "      name: \"Without plan\"\n" +
        "  - Composite:\n" +
        "      name: \"With plan\"\n" +
        "      plan:\n" +
        "        root:\n" +
        "          sequence:\n" +
        "            children:\n" +
        "              - callPlan:\n" +
        "                  selectionAttributes:\n" +
        "                    - name: \"SubPlan\"\n" +
        "                    - env: \"T1\"\n" +
        "              - echo:\n" +
        "                  text: \"http://${host}\"\n" +
        "  - Composite:\n" +
        "      name: \"Last\"\n" +
        "      plan:\n" +
        "        root:\n" +
        "          sequence:\n" +
        "            children:\n" +
        "              - callPlan:\n" +
        "                  selectionAttributes:\n" +
        "                    - name: \"OtherPlan\"\n";

    private final AutomationPackageDescriptorReader reader;

    public AutomationPackageDescriptorReaderMigrationTest() {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageScheduleRegistration.registerSerialization(serializationRegistry);
        reader = new AutomationPackageDescriptorReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, serializationRegistry);
    }

    /**
     * The legacy beforeSequence, afterSequence, beforeThread and afterThread artefacts, as well as a performanceAssert
     * used as a child, are rejected by the current schema and have to be migrated first
     */
    @Test
    public void testLegacyBeforeAfterArtefactsAreMigrated() throws Exception {
        AutomationPackageDescriptorYaml descriptor;
        try (InputStream is = new FileInputStream(BEFORE_AFTER_OLD_VERSION_DESCRIPTOR)) {
            descriptor = reader.readAutomationPackageDescriptor(is, "test");
        }
        assertEquals(1, descriptor.getPlans().size());
        Plan plan = reader.getPlanReader().yamlPlanToPlan(descriptor.getPlans().get(0));

        ThreadGroup threadGroup = (ThreadGroup) plan.getRoot();
        assertEquals(1, threadGroup.getChildren().size());

        // Both legacy beforeThread artefacts are merged into the beforeThread block
        List<AbstractArtefact> beforeThread = threadGroup.getBeforeThread().getSteps();
        assertEquals(4, beforeThread.size());
        assertEquals("var", ((Set) beforeThread.get(0)).getKey().getValue());
        assertEquals("var2", ((Set) beforeThread.get(2)).getKey().getValue());

        List<AbstractArtefact> afterThread = threadGroup.getAfterThread().getSteps();
        assertEquals(1, afterThread.size());
        assertEquals("var", ((Echo) afterThread.get(0)).getText().getExpression());

        List<AbstractArtefact> after = threadGroup.getAfter().getSteps();
        assertEquals(1, after.size());
        assertTrue(after.get(0) instanceof PerformanceAssert);

        Sequence sequence = (Sequence) threadGroup.getChildren().get(0);
        assertEquals(1, sequence.getChildren().size());
        assertEquals(1, sequence.getBefore().getSteps().size());
        assertEquals("var", ((Echo) sequence.getBefore().getSteps().get(0)).getText().getExpression());
        assertEquals(1, sequence.getAfter().getSteps().size());
        assertEquals("var2", ((Echo) sequence.getAfter().getSteps().get(0)).getText().getExpression());
    }

    /**
     * The plans are migrated along with the descriptor and must not be migrated a second time when read, which would
     * for instance escape their values twice
     */
    @Test
    public void testPlansAreMigratedOnlyOnce() throws Exception {
        Plan plan = readSinglePlan(
            "version: 1.2.0\n" +
            "name: \"escaping\"\n" +
            "plans:\n" +
            "  - name: \"escaping\"\n" +
            "    root:\n" +
            "      sequence:\n" +
            "        children:\n" +
            "          - echo:\n" +
            "              text: \"http://${host}\"\n");

        Echo echo = (Echo) plan.getRoot().getChildren().get(0);
        assertEquals("http://$${host}", echo.getText().getValue());
    }

    @Test
    public void testPlansOfCurrentVersionAreNotMigrated() throws Exception {
        Plan plan = readSinglePlan(
            "version: " + YamlAutomationPackageVersions.ACTUAL_VERSION + "\n" +
            "name: \"escaping\"\n" +
            "plans:\n" +
            "  - name: \"escaping\"\n" +
            "    root:\n" +
            "      sequence:\n" +
            "        children:\n" +
            "          - echo:\n" +
            "              text: \"http://${host}\"\n");

        Echo echo = (Echo) plan.getRoot().getChildren().get(0);
        assertEquals("http://${host}", echo.getText().getValue());
    }

    /**
     * The plan of a composite keyword is a yaml plan as well and is migrated along with the plans of the package. The
     * keywords before and after it, with or without plan, must stay in place
     */
    @Test
    public void testCompositeKeywordPlansAreMigrated() throws Exception {
        AutomationPackageDescriptorYaml descriptor;
        try (InputStream is = new ByteArrayInputStream(("version: 1.2.0\n" +
            "name: \"composites\"\n" +
            COMPOSITES_OLD_VERSION).getBytes(StandardCharsets.UTF_8))) {
            descriptor = reader.readAutomationPackageDescriptor(is, "test");
        }
        assertMigratedComposites(descriptor.getKeywords());
    }

    /**
     * A fragment declaring no version of its own follows the one of its package, including for the plans of its
     * composite keywords
     */
    @Test
    public void testCompositeKeywordPlansOfFragmentsAreMigrated() throws Exception {
        AutomationPackageFragmentYaml fragment;
        try (InputStream is = new ByteArrayInputStream(COMPOSITES_OLD_VERSION.getBytes(StandardCharsets.UTF_8))) {
            fragment = reader.readAutomationPackageFragment(is, "keywords.yml", "test", "1.2.0");
        }
        assertMigratedComposites(fragment.getKeywords());
    }

    private void assertMigratedComposites(List<YamlAutomationPackageKeyword> keywords) throws Exception {
        assertEquals(3, keywords.size());
        assertNull(((YamlCompositeFunction) keywords.get(0).getYamlKeyword()).getPlan());

        Plan plan = compositePlan(keywords.get(1));
        JsonNode selection = new ObjectMapper().readTree(((CallPlan) plan.getRoot().getChildren().get(0)).getSelectionAttributes().getValue());
        assertEquals("SubPlan", selection.get("name").get("value").asText());
        assertEquals("T1", selection.get("env").get("value").asText());
        // The other yaml plan migrations apply as well, and only once
        assertEquals("http://$${host}", ((Echo) plan.getRoot().getChildren().get(1)).getText().getValue());

        CallPlan lastCallPlan = (CallPlan) compositePlan(keywords.get(2)).getRoot().getChildren().get(0);
        assertEquals("OtherPlan", new ObjectMapper().readTree(lastCallPlan.getSelectionAttributes().getValue()).get("name").get("value").asText());
    }

    private Plan compositePlan(YamlAutomationPackageKeyword keyword) {
        return reader.getPlanReader().yamlPlanToPlan(((YamlCompositeFunction) keyword.getYamlKeyword()).getPlan());
    }

    private Plan readSinglePlan(String yaml) throws AutomationPackageReadingException, IOException {
        try (InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
            AutomationPackageDescriptorYaml descriptor = reader.readAutomationPackageDescriptor(is, "test");
            assertEquals(1, descriptor.getPlans().size());
            return reader.getPlanReader().yamlPlanToPlan(descriptor.getPlans().get(0));
        }
    }
}
