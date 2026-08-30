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
package step.plans.parser.yaml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.CallPlan;
import step.artefacts.reports.EchoReportNode;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.BasePlugin;
import step.threadpool.ThreadPoolPlugin;

/**
 * The values of a plan written against a schema older than 1.3.0 were used literally. After the introduction of the
 * string interpolation they must keep resolving to exactly what they did, which is what the escaping migration
 * guarantees. Bumping the schema version is how an author opts into the interpolation.
 */
public class YamlEscapingMigrationTest {

    private static final String PLACEHOLDERS =
        "  - set:\n" +
        "      key: \"greeting\"\n" +
        "      value: \"Hello ${name}\"\n" +
        "  - echo:\n" +
        "      text: \"${greeting} costs $$5\"\n";

    private final YamlPlanReader yamlPlanReader = new YamlPlanReader();

    // Plans written against the old schema

    /**
     * Note the {@code $$}: before the interpolation existed it was simply two literal dollars, so it has to survive
     * as two dollars. Escaping only {@code ${} would silently turn it into one
     */
    @Test
    public void testOldPlanKeepsItsLiteralValues() throws Exception {
        Plan plan = readPlan("1.2.0", PLACEHOLDERS);
        Assert.assertEquals(List.of("${greeting} costs $$5"), execute(plan));
    }

    @Test
    public void testOldPlanWithoutPlaceholderIsUntouched() throws Exception {
        Plan plan = readPlan("1.2.0",
            "  - echo:\n" +
            "      text: \"nothing to escape here\"\n");
        Assert.assertEquals(List.of("nothing to escape here"), execute(plan));
    }

    /**
     * Values written as an expression were already evaluated as groovy and must not be escaped
     */
    @Test
    public void testOldPlanExpressionsStillEvaluate() throws Exception {
        Plan plan = readPlan("1.2.0",
            "  - set:\n" +
            "      key: \"name\"\n" +
            "      value: \"Jane\"\n" +
            "  - echo:\n" +
            "      text:\n" +
            "        expression: '\"Hello \" + name'\n");
        Assert.assertEquals(List.of("Hello Jane"), execute(plan));
    }

    @Test
    public void testOldPlanNestedInChildrenAndBeforeBlocks() throws Exception {
        Plan plan = readPlan("1.2.0",
            "  - sequence:\n" +
            "      before:\n" +
            "        steps:\n" +
            "          - echo:\n" +
            "              text: \"before ${x}\"\n" +
            "      children:\n" +
            "        - echo:\n" +
            "            text: \"nested ${y}\"\n");
        Assert.assertEquals(List.of("before ${x}", "nested ${y}"), execute(plan));
    }

    @Test
    public void testOldPlanKeywordInputsAreEscaped() throws Exception {
        Plan plan = readPlan("1.2.0",
            "  - callKeyword:\n" +
            "      keyword: \"My Keyword\"\n" +
            "      inputs:\n" +
            "        - url: \"http://${host}:8080\"\n" +
            "        - plain: \"nothing\"\n" +
            "        - count: 42\n");

        String argument = findArtefact(plan, CallFunction.class).getArgument().getValue();
        Assert.assertTrue(argument, argument.contains("http://$${host}:8080"));
        Assert.assertTrue(argument, argument.contains("nothing"));
    }

    @Test
    public void testOldPlanCallPlanInputsAreEscaped() throws Exception {
        Plan plan = readPlan("1.2.0",
            "  - callPlan:\n" +
            "      input:\n" +
            "        - p1: \"value ${v}\"\n");

        String input = findArtefact(plan, CallPlan.class).getInput().getValue();
        Assert.assertTrue(input, input.contains("value $${v}"));
    }

    // Plans written against the new schema: the author opted in

    @Test
    public void testNewPlanInterpolates() throws Exception {
        Plan plan = readPlan("1.3.0",
            "  - set:\n" +
            "      key: \"name\"\n" +
            "      value: \"Jane\"\n" +
            "  - echo:\n" +
            "      text: \"Hello ${name}\"\n");
        Assert.assertEquals(List.of("Hello Jane"), execute(plan));
    }

    @Test
    public void testNewPlanKeywordInputsAreNotEscaped() throws Exception {
        Plan plan = readPlan("1.3.0",
            "  - callKeyword:\n" +
            "      keyword: \"My Keyword\"\n" +
            "      inputs:\n" +
            "        - url: \"http://${host}:8080\"\n");

        String argument = findArtefact(plan, CallFunction.class).getArgument().getValue();
        Assert.assertTrue(argument, argument.contains("http://${host}:8080"));
    }

    private Plan readPlan(String version, String children) throws Exception {
        String yamlPlan = "version: " + version + "\n" +
            "name: \"escaping migration\"\n" +
            "root:\n" +
            "  sequence:\n" +
            "    children:\n" +
            children.lines().map(l -> l.isEmpty() ? l : "    " + l).reduce("", (a, b) -> a + b + "\n");
        try (InputStream is = new ByteArrayInputStream(yamlPlan.getBytes(StandardCharsets.UTF_8))) {
            return yamlPlanReader.readYamlPlan(is);
        }
    }

    private <T extends AbstractArtefact> T findArtefact(Plan plan, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(plan.getRoot(), type, found);
        Assert.assertEquals("Expected exactly one " + type.getSimpleName(), 1, found.size());
        return found.get(0);
    }

    private <T extends AbstractArtefact> void collect(AbstractArtefact artefact, Class<T> type, List<T> found) {
        if (type.isInstance(artefact)) {
            found.add(type.cast(artefact));
        }
        artefact.getChildren().forEach(child -> collect(child, type, found));
    }

    private List<String> execute(Plan plan) throws Exception {
        List<String> echoes = new ArrayList<>();
        try (ExecutionEngine engine = ExecutionEngine.builder()
            .withPlugin(new BaseArtefactPlugin())
            .withPlugin(new BasePlugin())
            .withPlugin(new ThreadPoolPlugin())
            .build()) {
            PlanRunnerResult result = engine.execute(plan, Map.of());
            result.waitForExecutionToTerminate();
            Assert.assertEquals(result.getErrorSummary(), ReportNodeStatus.PASSED, result.getResult());
            result.visitReportNodes(node -> {
                if (node instanceof EchoReportNode) {
                    echoes.add(((EchoReportNode) node).getEcho());
                }
            });
        }
        return echoes;
    }
}
