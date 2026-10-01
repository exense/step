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
package step.plans.parser.yaml.migrations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.CallPlan;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.plans.parser.yaml.YamlPlanReader;

/**
 * A call plan written against a schema older than 1.3.0 selects the called plan with the selectionAttributes field,
 * which the plan field replaces. Once migrated it must select exactly the same plan as when written with the new
 * syntax.
 */
public class V1_3_0_CallPlanSelectionAttributesYamlMigrationTaskTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final YamlPlanReader yamlPlanReader = new YamlPlanReader();

    @Test
    public void testSelectionByNameOnly() throws Exception {
        assertSameSelection(
            "      selectionAttributes:\n" +
            "        - name: \"SubPlan\"\n",
            "      plan: \"SubPlan\"\n");
    }

    @Test
    public void testSelectionByMultipleAttributes() throws Exception {
        assertSameSelection(
            "      selectionAttributes:\n" +
            "        - name: \"SubPlan\"\n" +
            "        - env: \"T1\"\n",
            "      plan:\n" +
            "        name: \"SubPlan\"\n" +
            "        env: \"T1\"\n");
    }

    @Test
    public void testSelectionWithDynamicValue() throws Exception {
        assertSameSelection(
            "      selectionAttributes:\n" +
            "        - name: \"SubPlan\"\n" +
            "        - env:\n" +
            "            expression: \"environment\"\n",
            "      plan:\n" +
            "        name: \"SubPlan\"\n" +
            "        env:\n" +
            "          expression: \"environment\"\n");
    }

    private void assertSameSelection(String oldCallPlanFields, String newCallPlanFields) throws Exception {
        DynamicValue<String> migrated = readCallPlan("1.2.0", oldCallPlanFields).getSelectionAttributes();
        DynamicValue<String> expected = readCallPlan("1.3.0", newCallPlanFields).getSelectionAttributes();

        Assert.assertFalse(migrated.isDynamic());
        Assert.assertEquals(json(expected.getValue()), json(migrated.getValue()));
    }

    private CallPlan readCallPlan(String version, String callPlanFields) throws Exception {
        String yamlPlan = "version: " + version + "\n" +
            "name: \"call plan migration\"\n" +
            "root:\n" +
            "  sequence:\n" +
            "    children:\n" +
            "      - callPlan:\n" +
            callPlanFields.lines().map(l -> "    " + l + "\n").reduce("", String::concat);
        try (InputStream is = new ByteArrayInputStream(yamlPlan.getBytes(StandardCharsets.UTF_8))) {
            Plan plan = yamlPlanReader.readYamlPlan(is);
            return (CallPlan) plan.getRoot().getChildren().get(0);
        }
    }

    private static JsonNode json(String value) throws Exception {
        return MAPPER.readTree(value);
    }
}
