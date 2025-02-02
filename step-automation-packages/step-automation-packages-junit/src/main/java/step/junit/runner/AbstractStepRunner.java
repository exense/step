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
import step.automation.packages.junit.JunitExecutionParametersProvider;
import step.cli.AbstractExecuteAutomationPackageTool;
import step.core.execution.ExecutionEngine;
import step.core.plans.runner.PlanRunnerResult;
import step.resources.ResourceManager;

import java.util.List;
import java.util.Map;

public abstract class AbstractStepRunner extends ParentRunner<StepClassParserResult> {

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
                return new JunitExecutionParametersProvider().getExecutionParameters(klass);
            }
        }.runPlan();
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
