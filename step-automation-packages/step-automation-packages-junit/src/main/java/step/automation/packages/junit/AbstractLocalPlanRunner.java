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
package step.automation.packages.junit;

import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunnerResult;
import step.junit.runner.StepClassParserResult;

import java.util.Map;

public abstract class AbstractLocalPlanRunner {

    public void runPlan(StepClassParserResult parserResult, ExecutionEngine executionEngine){
        onExecutionStart();

        try {
            Exception initializingException = parserResult.getInitializingException();
            if (initializingException == null) {
                Plan plan = parserResult.getPlan();
                Map<String, String> executionParameters = getExecutionParameters();
                PlanRunnerResult result = executionEngine.execute(plan, executionParameters);
                ReportNodeStatus resultStatus = result.getResult();

                if (resultStatus == ReportNodeStatus.PASSED) {
                    // We actually also want to see results when tests complete successfully
                    result.printTree();
                } else if (resultStatus == ReportNodeStatus.FAILED) {
                    onExecutionError(result, "Plan execution failed", true);
                } else if (resultStatus == ReportNodeStatus.TECHNICAL_ERROR) {
                    onExecutionError(result, "Technical error while executing plan", false);
                } else {
                    onExecutionError(result, "The plan execution returned an unexpected status\" + result", false);
                }
            } else {
               onInitializingException(initializingException);
            }
        } catch (Exception e) {
            onExecutionException(e);
        } finally {
            onTestFinished();
        }
    }

    protected abstract void onExecutionStart();

    protected abstract void onExecutionError(PlanRunnerResult result, String errorText, boolean assertionError);

    protected abstract void onInitializingException(Exception exception);

    protected abstract void onExecutionException(Exception exception);

    protected abstract void onTestFinished();

    // TODO: use execution parameters from env and system properties in CLI?
    protected abstract Map<String, String> getExecutionParameters();

}
