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

import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.DocumentObject;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.migration.MigrationContext;

import static step.plans.parser.yaml.migrations.AbstractYamlPlanMigrationTask.YAML_PLANS_COLLECTION_NAME;

/**
 * Unit test of the migration task itself, driving it directly on the documents of the yamlPlans collection.
 * <p>
 * The end to end behaviour, from the yaml source down to the call plan artefact, is covered by the
 * V1_3_0_CallPlanSelectionAttributesYamlMigrationTaskTest of the step-plans-yaml-parser module.
 */
public class V1_3_0_CallPlanSelectionAttributesYamlMigrationTaskUnitTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InMemoryCollectionFactory collectionFactory = new InMemoryCollectionFactory(new Properties());
    private final Collection<Document> yamlPlans = collectionFactory.getCollection(YAML_PLANS_COLLECTION_NAME, Document.class);
    private final V1_3_0_CallPlanSelectionAttributesYamlMigrationTask task =
        new V1_3_0_CallPlanSelectionAttributesYamlMigrationTask(collectionFactory, new MigrationContext());

    @Test
    public void testTaskAppliesAsOfTheSchemaVersionIntroducingThePlanField() {
        Assert.assertEquals("1.3.0", task.getAsOfVersion().toString());
    }

    @Test
    public void testSelectionByNameOnlyBecomesTheSimpleForm() {
        Document migrated = migrate(planWithChildren("{'callPlan':{'selectionAttributes':[{'name':'SubPlan'}]}}"));

        DocumentObject callPlan = callPlan(migrated, 0);
        Assert.assertEquals("SubPlan", callPlan.get("plan"));
        Assert.assertFalse(callPlan.containsKey("selectionAttributes"));
    }

    @Test
    public void testSelectionByMultipleAttributesBecomesTheMapFormInTheSameOrder() {
        Document migrated = migrate(planWithChildren(
            "{'callPlan':{'selectionAttributes':[{'name':'SubPlan'},{'env':'T1'},{'app':'A'}]}}"));

        DocumentObject callPlan = callPlan(migrated, 0);
        DocumentObject plan = callPlan.getObject("plan");
        Assert.assertEquals(List.of("name", "env", "app"), List.copyOf(plan.keySet()));
        Assert.assertEquals("SubPlan", plan.get("name"));
        Assert.assertEquals("T1", plan.get("env"));
        Assert.assertEquals("A", plan.get("app"));
        Assert.assertFalse(callPlan.containsKey("selectionAttributes"));
    }

    @Test
    public void testSelectionWithoutNameBecomesTheMapForm() {
        Document migrated = migrate(planWithChildren("{'callPlan':{'selectionAttributes':[{'env':'T1'}]}}"));

        Assert.assertEquals("T1", callPlan(migrated, 0).getObject("plan").get("env"));
    }

    /**
     * A dynamic name can't be written with the simple form, which only holds a plain name
     */
    @Test
    public void testDynamicNameBecomesTheMapForm() {
        Document migrated = migrate(planWithChildren(
            "{'callPlan':{'selectionAttributes':[{'name':{'expression':'planName'}}]}}"));

        DocumentObject name = callPlan(migrated, 0).getObject("plan").getObject("name");
        Assert.assertEquals("planName", name.get("expression"));
    }

    @Test
    public void testDynamicValuesAreKept() {
        Document migrated = migrate(planWithChildren(
            "{'callPlan':{'selectionAttributes':[{'name':'SubPlan'},{'env':{'expression':'environment'}}]}}"));

        DocumentObject plan = callPlan(migrated, 0).getObject("plan");
        Assert.assertEquals("SubPlan", plan.get("name"));
        Assert.assertEquals("environment", plan.getObject("env").get("expression"));
    }

    @Test
    public void testOtherFieldsOfTheCallPlanAreLeftUntouched() {
        Document migrated = migrate(planWithChildren(
            "{'callPlan':{'nodeName':'Call','selectionAttributes':[{'name':'SubPlan'}],'input':[{'p1':'v1'}]}}"));

        DocumentObject callPlan = callPlan(migrated, 0);
        Assert.assertEquals("Call", callPlan.get("nodeName"));
        Assert.assertEquals("v1", callPlan.getArray("input").get(0).get("p1"));
        Assert.assertEquals("SubPlan", callPlan.get("plan"));
    }

    @Test
    public void testEmptySelectionAttributesAreRemoved() {
        Document migrated = migrate(planWithChildren("{'callPlan':{'selectionAttributes':[]}}"));

        DocumentObject callPlan = callPlan(migrated, 0);
        Assert.assertFalse(callPlan.containsKey("selectionAttributes"));
        Assert.assertFalse(callPlan.containsKey("plan"));
    }

    @Test
    public void testSelectionAttributesOfOtherArtefactsAreLeftUntouched() {
        Document migrated = migrate(planWithChildren("{'echo':{'text':'hello','selectionAttributes':[{'name':'x'}]}}"));

        DocumentObject echo = child(migrated, 0).getObject("echo");
        Assert.assertEquals("x", echo.getArray("selectionAttributes").get(0).get("name"));
        Assert.assertFalse(echo.containsKey("plan"));
    }

    /**
     * A call plan may be nested in the children of another artefact, or in one of the before/after blocks, which hold
     * their artefacts under a steps field
     */
    @Test
    public void testNestedChildrenAndBeforeAfterBlocksAreWalked() {
        Document migrated = migrate(planWithChildren(
            "{'sequence':{" +
            "'before':{'steps':[" + callPlanJson("before") + "]}," +
            "'after':{'steps':[" + callPlanJson("after") + "]}," +
            "'beforeThread':{'steps':[" + callPlanJson("beforeThread") + "]}," +
            "'afterThread':{'steps':[" + callPlanJson("afterThread") + "]}," +
            "'children':[{'sequence':{'children':[" + callPlanJson("nested") + "]}}]}}"));

        DocumentObject sequence = child(migrated, 0).getObject("sequence");
        for (String block : List.of("before", "after", "beforeThread", "afterThread")) {
            Assert.assertEquals(block, sequence.getObject(block).getArray("steps").get(0).getObject("callPlan").get("plan"));
        }
        DocumentObject nested = sequence.getArray("children").get(0).getObject("sequence");
        Assert.assertEquals("nested", nested.getArray("children").get(0).getObject("callPlan").get("plan"));
    }

    /**
     * A plan already using the plan field has nothing to migrate
     */
    @Test
    public void testCallPlanWithoutSelectionAttributesIsLeftUntouched() {
        Document migrated = migrate(planWithChildren("{'callPlan':{'plan':{'name':'SubPlan','env':'T1'}}}"));

        DocumentObject plan = callPlan(migrated, 0).getObject("plan");
        Assert.assertEquals("SubPlan", plan.get("name"));
        Assert.assertEquals("T1", plan.get("env"));
    }

    @Test
    public void testPlanWithoutRootIsIgnored() {
        Document migrated = migrate(plan("{'name':'no root'}"));

        Assert.assertNull(migrated.get("root"));
        Assert.assertEquals("no root", migrated.getString("name"));
    }

    /**
     * A plan the task cannot walk must not interrupt the migration: the other plans of the collection are still
     * migrated and the faulty one is left as it was.
     * <p>
     * The task logs the failure, so an error stack trace is expected in the output of this test
     */
    @Test
    public void testFaultyPlanIsReportedAndTheOtherPlansAreStillMigrated() {
        yamlPlans.save(plan("{'name':'faulty','root':'not an artefact'}"));
        yamlPlans.save(planWithName("sane", callPlanJson("SubPlan")));

        task.runUpgradeScript();

        Assert.assertEquals("not an artefact", byName("faulty").get("root"));
        Assert.assertEquals("SubPlan", callPlan(byName("sane"), 0).get("plan"));
    }

    private static String callPlanJson(String planName) {
        return "{'callPlan':{'selectionAttributes':[{'name':'" + planName + "'}]}}";
    }

    private Document migrate(Document plan) {
        yamlPlans.save(plan);
        task.runUpgradeScript();
        return yamlPlans.find(Filters.empty(), null, null, null, 0).findFirst()
            .orElseThrow(() -> new AssertionError("No plan found"));
    }

    private Document byName(String name) {
        return yamlPlans.find(Filters.empty(), null, null, null, 0)
            .filter(document -> name.equals(document.getString("name")))
            .findFirst().orElseThrow(() -> new AssertionError("No plan named " + name));
    }

    private static Document planWithChildren(String childrenJson) {
        return planWithName("plan", childrenJson);
    }

    private static Document planWithName(String name, String childrenJson) {
        return plan("{'name':'" + name + "','root':{'sequence':{'children':[" + childrenJson + "]}}}");
    }

    private static Document plan(String json) {
        try {
            return new Document(MAPPER.readValue(json.replace('\'', '"'), new TypeReference<Map<String, Object>>() {
            }));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static DocumentObject child(Document plan, int index) {
        return plan.getObject("root").getObject("sequence").getArray("children").get(index);
    }

    private static DocumentObject callPlan(Document plan, int index) {
        return child(plan, index).getObject("callPlan");
    }
}
