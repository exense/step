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
 * The end to end behaviour, from the yaml source down to the executed plan, is covered by the
 * EscapeStringInterpolationYamlMigrationTaskTest of the step-plans-yaml-parser module. This test focuses on the
 * document walking: which fields are escaped, which are left alone, and how a document the task cannot walk is handled.
 * <p>
 * Note that the in memory collection clones the entities it stores, so a value read back from the collection is one
 * the task actually saved, not the instance it modified.
 */
public class EscapeStringInterpolationYamlMigrationTaskUnitTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InMemoryCollectionFactory collectionFactory = new InMemoryCollectionFactory(new Properties());
    private final Collection<Document> yamlPlans = collectionFactory.getCollection(YAML_PLANS_COLLECTION_NAME, Document.class);
    private final EscapeStringInterpolationYamlMigrationTask task =
        new EscapeStringInterpolationYamlMigrationTask(collectionFactory, new MigrationContext());

    @Test
    public void testTaskAppliesAsOfTheSchemaVersionIntroducingTheInterpolation() {
        Assert.assertEquals("1.3.0", task.getAsOfVersion().toString());
    }

    @Test
    public void testScalarValuesAreEscaped() {
        Document migrated = migrate(planWithChildren(
            "{'echo':{'text':'Hello ${name}'}}," +
            "{'set':{'key':'greeting ${k}','value':'Hello ${name}'}}"));

        Assert.assertEquals("Hello $${name}", field(migrated, 0, "echo", "text"));
        Assert.assertEquals("greeting $${k}", field(migrated, 1, "set", "key"));
        Assert.assertEquals("Hello $${name}", field(migrated, 1, "set", "value"));
    }

    /**
     * Only the values that would change meaning are escaped, the others must be kept strictly as they were authored
     */
    @Test
    public void testValuesWithoutPlaceholderAreLeftUntouched() {
        Document migrated = migrate(planWithChildren(
            "{'echo':{'text':'nothing to escape'}}," +
            "{'echo':{'text':'pid $$ costs $5'}}"));

        Assert.assertEquals("nothing to escape", field(migrated, 0, "echo", "text"));
        Assert.assertEquals("pid $$ costs $5", field(migrated, 1, "echo", "text"));
    }

    @Test
    public void testExpressionValuesAreNotEscaped() {
        Document migrated = migrate(planWithChildren("{'echo':{'text':{'expression':'greeting + name'}}}"));

        DocumentObject text = child(migrated, 0).getObject("echo").getObject("text");
        Assert.assertEquals("greeting + name", text.get("expression"));
    }

    /**
     * The fields not listed by the task either belong to a later schema or don't end up in a DynamicValue. The field
     * set is frozen, so they must be left alone even when they do contain a placeholder
     */
    @Test
    public void testFieldsOutsideOfTheFrozenSetAreNotEscaped() {
        Document migrated = migrate(planWithChildren(
            "{'echo':{'text':'echoed ${x}','unknownField':'unknown ${y}'}}," +
            "{'sleep':{'duration':'${d}'}}"));

        Assert.assertEquals("echoed $${x}", field(migrated, 0, "echo", "text"));
        Assert.assertEquals("unknown ${y}", field(migrated, 0, "echo", "unknownField"));
        Assert.assertEquals("${d}", field(migrated, 1, "sleep", "duration"));
    }

    /**
     * The keyword inputs are written as a list of named values which the deserializer packs into a single json
     * document, so each of their values is escaped individually
     */
    @Test
    public void testKeywordInputsAreEscaped() {
        Document migrated = migrate(planWithChildren(
            "{'callKeyword':{'keyword':'My Keyword','inputs':[" +
            "{'url':'http://${host}:8080'},{'plain':'nothing'},{'count':42}]}}"));

        List<DocumentObject> inputs = child(migrated, 0).getObject("callKeyword").getArray("inputs");
        Assert.assertEquals("http://$${host}:8080", inputs.get(0).get("url"));
        Assert.assertEquals("nothing", inputs.get(1).get("plain"));
        // Not a string, so not a value the interpolation could apply to
        Assert.assertEquals(42, inputs.get(2).get("count"));
        // The keyword name is not part of the frozen field set
        Assert.assertEquals("My Keyword", field(migrated, 0, "callKeyword", "keyword"));
    }

    /**
     * A keyword input written as an expression is a groovy expression, which was already evaluated as such before the
     * interpolation existed. Its placeholders belong to groovy and must be kept as they are, including when a plain
     * input of the same list is escaped and the whole list is therefore rewritten
     */
    @Test
    public void testKeywordInputsWrittenAsExpressionsAreNotEscaped() {
        Document migrated = migrate(planWithChildren(
            "{'callKeyword':{'keyword':'My Keyword','inputs':[" +
            "{'url':{'expression':'\\\"http://${host}\\\"'}}," +
            "{'plain':'http://${host}'}]}}"));

        List<DocumentObject> inputs = child(migrated, 0).getObject("callKeyword").getArray("inputs");
        Assert.assertEquals("\"http://${host}\"", inputs.get(0).getObject("url").get("expression"));
        Assert.assertEquals("http://$${host}", inputs.get(1).get("plain"));
    }

    @Test
    public void testCallPlanInputsAreEscaped() {
        Document migrated = migrate(planWithChildren("{'callPlan':{'input':[{'p1':'value ${v}'},{'p2':'plain'}]}}"));

        List<DocumentObject> inputs = child(migrated, 0).getObject("callPlan").getArray("input");
        Assert.assertEquals("value $${v}", inputs.get(0).get("p1"));
        Assert.assertEquals("plain", inputs.get(1).get("p2"));
    }

    /**
     * A list of named values holding nothing to escape must not be rewritten at all
     */
    @Test
    public void testNamedValuesWithNothingToEscapeAreNotRewritten() {
        Document migrated = migrate(planWithChildren(
            "{'callKeyword':{'keyword':'My Keyword','inputs':[{'plain':'nothing'},{'count':42}]}}"));

        List<DocumentObject> inputs = child(migrated, 0).getObject("callKeyword").getArray("inputs");
        Assert.assertEquals("nothing", inputs.get(0).get("plain"));
        Assert.assertEquals(42, inputs.get(1).get("count"));
    }

    /**
     * An artefact may be nested in the children of another one, or in one of the before/after blocks, which hold
     * their artefacts under a steps field
     */
    @Test
    public void testNestedChildrenAndBeforeAfterBlocksAreWalked() {
        Document migrated = migrate(planWithChildren(
            "{'sequence':{" +
            "'before':{'steps':[{'echo':{'text':'before ${a}'}}]}," +
            "'after':{'steps':[{'echo':{'text':'after ${b}'}}]}," +
            "'beforeThread':{'steps':[{'echo':{'text':'beforeThread ${c}'}}]}," +
            "'afterThread':{'steps':[{'echo':{'text':'afterThread ${d}'}}]}," +
            "'children':[{'sequence':{'children':[{'echo':{'text':'deeply nested ${e}'}}]}}]}}"));

        DocumentObject sequence = child(migrated, 0).getObject("sequence");
        Assert.assertEquals("before $${a}", stepText(sequence, "before"));
        Assert.assertEquals("after $${b}", stepText(sequence, "after"));
        Assert.assertEquals("beforeThread $${c}", stepText(sequence, "beforeThread"));
        Assert.assertEquals("afterThread $${d}", stepText(sequence, "afterThread"));

        DocumentObject nested = sequence.getArray("children").get(0).getObject("sequence");
        Assert.assertEquals("deeply nested $${e}", nested.getArray("children").get(0).getObject("echo").get("text"));
    }

    @Test
    public void testEmptyAndMissingListsAreIgnored() {
        Document migrated = migrate(planWithChildren(
            "{'sequence':{'children':[],'before':{'steps':[]}}}," +
            "{'callKeyword':{'keyword':'No Inputs'}}," +
            "{'callKeyword':{'keyword':'Empty Inputs','inputs':[]}}"));

        Assert.assertEquals(List.of(), child(migrated, 0).getObject("sequence").getArray("children"));
        Assert.assertEquals("No Inputs", field(migrated, 1, "callKeyword", "keyword"));
        Assert.assertEquals(List.of(), child(migrated, 2).getObject("callKeyword").getArray("inputs"));
    }

    @Test
    public void testArtefactWithoutPropertiesIsIgnored() {
        Document migrated = migrate(plan("{'name':'no properties','root':{'sequence':null}}"));

        Assert.assertNull(migrated.getObject("root").get("sequence"));
    }

    @Test
    public void testPlanWithoutRootIsIgnored() {
        Document migrated = migrate(plan("{'name':'no root'}"));

        Assert.assertNull(migrated.get("root"));
        Assert.assertEquals("no root", migrated.getString("name"));
    }

    /**
     * A plan the task cannot walk must neither interrupt the migration nor be half rewritten: the other plans of the
     * collection are still migrated and the faulty one is left as it was.
     * <p>
     * The task logs the failure, so an error stack trace is expected in the output of this test
     */
    @Test
    public void testFaultyPlanIsReportedAndTheOtherPlansAreStillMigrated() {
        yamlPlans.save(plan("{'name':'faulty','root':'not an artefact'}"));
        yamlPlans.save(planWithName("sane", "{'echo':{'text':'sane ${x}'}}"));

        task.runUpgradeScript();

        Assert.assertEquals("not an artefact", byName("faulty").get("root"));
        Assert.assertEquals("sane $${x}", field(byName("sane"), 0, "echo", "text"));
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

    private static Object field(Document plan, int index, String artefactName, String field) {
        return child(plan, index).getObject(artefactName).get(field);
    }

    private static Object stepText(DocumentObject artefact, String block) {
        return artefact.getObject(block).getArray("steps").get(0).getObject("echo").get("text");
    }
}
