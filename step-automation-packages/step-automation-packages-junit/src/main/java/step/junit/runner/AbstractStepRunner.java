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
package step.junit.runner;

import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import step.automation.packages.junit.AbstractLocalPlanRunner;
import step.cli.AbstractExecuteAutomationPackageTool;
import step.core.execution.ExecutionEngine;
import step.core.plans.runner.PlanRunnerResult;
import step.junit.runners.annotations.ExecutionParameters;
import step.resources.ResourceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractStepRunner extends ParentRunner<StepClassParserResult> {
    private final static Pattern SYSTEM_PROPERTIES_PREFIX = Pattern.compile("STEP_(.+?)");

    protected final Class<?> klass;
    protected List<StepClassParserResult> listPlans;
    protected ExecutionEngine executionEngine;
    protected ResourceManager resourceManager;

    public AbstractStepRunner(Class<?> testClass, Class<?> klass) throws InitializationError {
        super(testClass);
        this.klass = klass;
    }

    @Override
    protected Description describeChild(StepClassParserResult child) {
        return Description.createTestDescription(klass, child.getName());
    }

    @Override
    protected Statement childrenInvoker(RunNotifier notifier) {
        notifier.addListener(new RunListener() {
            @Override
            public void testSuiteFinished(Description description) throws Exception {
                // Close the execution engine once all tests were executed
                executionEngine.close();
                super.testSuiteFinished(description);
            }
        });
        return super.childrenInvoker(notifier);
    }

    @Override
    protected void runChild(StepClassParserResult child, RunNotifier notifier) {
        Description desc = Description.createTestDescription(klass, child.getName());
        EachTestNotifier childNotifier = new EachTestNotifier(notifier, desc);

        new AbstractLocalPlanRunner(child, executionEngine) {
            @Override
            protected void onExecutionStart() {
                childNotifier.fireTestStarted();
            }

            @Override
            protected void onExecutionError(PlanRunnerResult result, String errorText, boolean assertionError) {
                notifyFailure(childNotifier, result, errorText, assertionError);
            }

            @Override
            protected void onInitializingException(Exception exception) {
                childNotifier.addFailure(exception);
            }

            @Override
            protected void onExecutionException(Exception exception) {
                childNotifier.addFailure(exception);
            }

            @Override
            protected void onTestFinished() {
                childNotifier.fireTestFinished();
            }

            @Override
            protected Map<String, String> getExecutionParameters() {
                return AbstractStepRunner.this.getExecutionParameters();
            }
        }.runPlan();
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
        if ((params = klass.getAnnotation(ExecutionParameters.class)) != null) {
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

    protected void notifyFailure(EachTestNotifier childNotifier, PlanRunnerResult res, String errorMsg,
                                 boolean assertionError) {
        String executionTree = AbstractExecuteAutomationPackageTool.getExecutionTreeAsString(res);
        String detailMessage = errorMsg + "\nExecution tree is:\n" + executionTree;
        if (assertionError) {
            childNotifier.addFailure(new AssertionError(detailMessage));
        } else {
            childNotifier.addFailure(new Exception(detailMessage));
        }
    }

    @Override
    protected List<StepClassParserResult> getChildren() {
        return listPlans;
    }
}
