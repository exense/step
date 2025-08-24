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
import step.automation.packages.junit.AbstractLocalPlanRunner;
import step.automation.packages.junit.JUnitExecutionParametersProvider;
import step.automation.packages.junit.JUnitPlansProvider;
import step.cli.ExecuteAutomationPackageTool;
import step.core.execution.ExecutionEngine;
import step.core.plans.runner.PlanRunnerResult;
import step.junit.runner.StepClassParserResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class StepJUnit5 {

    private static ExecutionEngine executionEngine;

    @BeforeAll
    public static void setupExecutionEngine() {
        executionEngine = ExecutionEngine.builder().withPluginsFromClasspath().build();
    }

    @AfterAll
    public static void destroyExecutionEngine() {
        if (executionEngine != null) {
            executionEngine.close();
        }
    }

    @TestFactory
    public List<DynamicNode> plans() {
        Class<? extends StepJUnit5> testClass = this.getClass();
        List<StepClassParserResult> testPlans = new JUnitPlansProvider(testClass).getTestPlans(executionEngine);
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
                    return new JUnitExecutionParametersProvider().getExecutionParameters(testClass);
                }
            }.runPlan()));
        }
        return tests;
    }

    protected void notifyFailure(PlanRunnerResult res, String errorMsg, boolean assertionError) {
        String executionTree = ExecuteAutomationPackageTool.getExecutionTreeAsString(res);
        String detailMessage = errorMsg + "\nExecution tree is:\n" + executionTree;
        if (assertionError) {
            throw new AssertionError(detailMessage);
        } else {
            throw new RuntimeException(detailMessage);
        }
    }

}
