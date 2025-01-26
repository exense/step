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
package step.junit5.runner;

import org.junit.jupiter.api.*;
import step.automation.packages.junit.*;
import step.cli.AbstractExecuteAutomationPackageTool;
import step.core.execution.ExecutionEngine;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.handlers.javahandler.AbstractKeyword;
import step.junit.runner.StepClassParserResult;
import step.junit.runners.annotations.ExecutionParameters;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StepJUnit5 extends AbstractKeyword {

    private final static Pattern SYSTEM_PROPERTIES_PREFIX = Pattern.compile("STEP_(.+?)");

    private static ExecutionEngine executionEngine;

    @BeforeAll
    public static void setupExecutionEngine() {
        AbstractExecutionEnginePlugin plugin = new AbstractExecutionEnginePlugin() {
        };
        executionEngine = ExecutionEngine.builder().withPlugin(plugin).withPluginsFromClasspath().build();
    }

    @AfterAll
    public static void destroyExecutionEngine() {
        if (executionEngine != null) {
            executionEngine.close();
        }
    }

    @TestFactory
    public List<DynamicNode> plans() {
        List<StepClassParserResult> testPlans = new JUnitPlansProvider().getTestPlans(executionEngine);
        List<DynamicNode> tests = new ArrayList<>();
        for (StepClassParserResult testPlan : testPlans) {
            tests.add(DynamicTest.dynamicTest(testPlan.getName(), () -> new AbstractLocalPlanRunner(testPlan, executionEngine) {
                @Override
                protected void onExecutionStart() {
                }

                @Override
                protected void onExecutionError(PlanRunnerResult result, String errorText, boolean assertionError) {
                    notifyFailure(result, errorText, assertionError);
                }

                @Override
                protected void onInitializingException(Exception exception) {
                    throw new RuntimeException(exception);
                }

                @Override
                protected void onExecutionException(Exception exception) {
                    throw new RuntimeException(exception);
                }

                @Override
                protected void onTestFinished() {

                }

                @Override
                protected Map<String, String> getExecutionParameters() {
                    return StepJUnit5.this.getExecutionParameters();
                }
            }.runPlan()));
        }
        return tests;
    }

    protected Map<String, String> getExecutionParameters() {
        HashMap<String, String> executionParameters = new HashMap<>();
        // Prio 3: Execution parameters from annotation ExecutionParameters
        executionParameters.putAll(getExecutionParametersByAnnotation());
        // Prio 2: Execution parameters from environment variables (prefixed with STEP_*)
        executionParameters.putAll(getExecutionParametersFromEnvironmentVariables());
        // Prio 3: Execution parameters from system properties
        executionParameters.putAll(getExecutionParametersFromSystemProperties());
        return executionParameters;
    }

    private Map<String, String> getExecutionParametersByAnnotation() {
        Map<String, String> executionParameters = new HashMap<>();
        ExecutionParameters params;
        if ((params = this.getClass().getAnnotation(ExecutionParameters.class)) != null) {
            String key = null;
            for (String param : params.value()) {
                if (key == null) {
                    key = param;
                } else {
                    executionParameters.put(key, param);
                    key = null;
                }
            }
        }
        return executionParameters;
    }

    protected Map<String, String> getExecutionParametersFromSystemProperties() {
        Map<String, String> executionParameters = new HashMap<>();
        System.getProperties().forEach((k, v) ->
                unescapeParameterKeyIfMatches(k.toString()).ifPresent(key -> executionParameters.put(key, v.toString())));
        return executionParameters;
    }

    private Map<String, String> getExecutionParametersFromEnvironmentVariables() {
        Map<String, String> executionParameters = new HashMap<>();
        System.getenv().forEach((k, v) -> unescapeParameterKeyIfMatches(k).ifPresent(key -> executionParameters.put(key, v)));
        return executionParameters;
    }

    private Optional<String> unescapeParameterKeyIfMatches(String key) {
        Matcher matcher = SYSTEM_PROPERTIES_PREFIX.matcher(key);
        if (matcher.matches()) {
            String unescapedKey = matcher.group(1);
            return Optional.of(unescapedKey);
        } else {
            return Optional.empty();
        }
    }

    protected void notifyFailure(PlanRunnerResult res, String errorMsg, boolean assertionError) {
        String executionTree = AbstractExecuteAutomationPackageTool.getExecutionTreeAsString(res);
        String detailMessage = errorMsg + "\nExecution tree is:\n" + executionTree;
        if (assertionError) {
            throw new AssertionError(detailMessage);
        } else {
            throw new RuntimeException(detailMessage);
        }
    }


}
