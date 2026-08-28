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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.BaseArtefactPlugin;
import step.artefacts.reports.EchoReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.engine.plugins.BasePlugin;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunnerResult;
import step.threadpool.ThreadPoolPlugin;

/**
 * End to end tests of the string interpolation of plain values in YAML plans. The main purpose of the interpolation
 * is to avoid having to fall back to the verbose {@code expression:} form just to inject a variable into a value.
 */
public class YamlPlanStringInterpolationTest {

    private final YamlPlanReader yamlPlanReader = new YamlPlanReader();

    /**
     * The plain form and the expression form must produce the same result
     */
    @Test
    public void testPlainValueIsInterpolated() throws Exception {
        Plan plan = readPlan(
            "  - set:\n" +
            "      key: \"host\"\n" +
            "      value: \"myhost\"\n" +
            "  - echo:\n" +
            "      text: \"http://${host}:8080/api\"\n" +
            "  - echo:\n" +
            "      text:\n" +
            "        expression: '\"http://\" + host + \":8080/api\"'\n");

        Assert.assertEquals(List.of("http://myhost:8080/api", "http://myhost:8080/api"), execute(plan, null));
    }

    @Test
    public void testInterpolationOfKeywordInputs() throws Exception {
        Plan plan = readPlan(
            "  - set:\n" +
            "      key: \"productId\"\n" +
            "      value: \"42\"\n" +
            "  - echo:\n" +
            "      text: \"/products/${productId}/details\"\n");

        Assert.assertEquals(List.of("/products/42/details"), execute(plan, null));
    }

    /**
     * Execution parameters are available to the interpolation like any other variable
     */
    @Test
    public void testInterpolationOfExecutionParameters() throws Exception {
        Plan plan = readPlan(
            "  - echo:\n" +
            "      text: \"Running on ${env}\"\n");

        Assert.assertEquals(List.of("Running on PROD"), execute(plan, Map.of("env", "PROD")));
    }

    @Test
    public void testSetValueIsInterpolated() throws Exception {
        Plan plan = readPlan(
            "  - set:\n" +
            "      key: \"firstName\"\n" +
            "      value: \"Jane\"\n" +
            "  - set:\n" +
            "      key: \"greeting\"\n" +
            "      value: \"Hello ${firstName}\"\n" +
            "  - echo:\n" +
            "      text: \"${greeting}!\"\n");

        Assert.assertEquals(List.of("Hello Jane!"), execute(plan, null));
    }

    /**
     * The YAML value is passed to the interpolation exactly as written: the characters which the groovy lexer would
     * otherwise interpret must survive
     */
    @Test
    public void testSpecialCharactersArePreserved() throws Exception {
        Plan plan = readPlan(
            "  - set:\n" +
            "      key: \"name\"\n" +
            "      value: \"Jane\"\n" +
            "  - echo:\n" +
            "      text: \"C:\\\\logs\\\\${name}.txt\"\n" +
            "  - echo:\n" +
            "      text: '{\"user\":\"${name}\"}'\n" +
            "  - echo:\n" +
            "      text: \"$${name}\"\n" +
            "  - echo:\n" +
            "      text: \"Price: $5\"\n");

        Assert.assertEquals(List.of("C:\\logs\\Jane.txt", "{\"user\":\"Jane\"}", "${name}", "Price: $5"),
            execute(plan, null));
    }

    /**
     * A plain multi line value, as produced by the YAML block scalar syntax, is interpolated too
     */
    @Test
    public void testMultilineValueIsInterpolated() throws Exception {
        Plan plan = readPlan(
            "  - set:\n" +
            "      key: \"name\"\n" +
            "      value: \"Jane\"\n" +
            "  - echo:\n" +
            "      text: |-\n" +
            "        line1\n" +
            "        line2 ${name}\n");

        Assert.assertEquals(List.of("line1\nline2 Jane"), execute(plan, null));
    }

    /**
     * Wraps the provided children into a plan. The children are written without their leading indentation in the
     * tests and are indented here to the level of the children list of the root sequence
     */
    private Plan readPlan(String children) throws IOException, step.plans.parser.yaml.schema.YamlPlanValidationException {
        StringBuilder yamlPlan = new StringBuilder("version: 1.0.0\n" +
            "name: \"string interpolation\"\n" +
            "root:\n" +
            "  sequence:\n" +
            "    children:\n");
        for (String line : children.split("\n", -1)) {
            yamlPlan.append(line.isEmpty() ? line : "    " + line).append("\n");
        }
        try (InputStream is = new ByteArrayInputStream(yamlPlan.toString().getBytes(StandardCharsets.UTF_8))) {
            return yamlPlanReader.readYamlPlan(is);
        }
    }

    /**
     * Executes the plan and returns the text echoed by each Echo node, in execution order
     */
    private List<String> execute(Plan plan, Map<String, String> executionParameters) throws IOException, TimeoutException, InterruptedException {
        List<String> echoes = new ArrayList<>();
        try (ExecutionEngine engine = ExecutionEngine.builder()
            .withPlugin(new BaseArtefactPlugin())
            // Makes the execution parameters available as variables
            .withPlugin(new BasePlugin())
            .withPlugin(new ThreadPoolPlugin())
            .build()) {
            PlanRunnerResult result = engine.execute(plan, executionParameters);
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
