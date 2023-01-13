package step.junit.runner;

import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunnerResult;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

public class StepPlanRunner extends Runner {

    private final String name;
    private final Plan plan;
    private final Exception initializingException;
    private final Map<String, String> executionParameters;
    private final ExecutionEngine executionEngine;
    private final Class klass;

    public StepPlanRunner(Class klass, String name, Plan plan, Map<String, String> executionParameters, ExecutionEngine executionEngine, Exception initializingException) {
        super();
        this.klass = klass;
        this.name = name;
        this.plan = plan;
        this.initializingException = initializingException;
        this.executionParameters = executionParameters;
        this.executionEngine = executionEngine;
    }

    @Override
    public Description getDescription() {
        return Description.createTestDescription(klass, name);
    }

    @Override
    public void run(RunNotifier runNotifier) {
        runNotifier.fireTestStarted(getDescription());
        try {
            if (initializingException == null) {
                PlanRunnerResult result = executionEngine.execute(plan, executionParameters);
                ReportNodeStatus resultStatus = result.getResult();

                Exception error = null;
                if (resultStatus == ReportNodeStatus.FAILED) {
                    error = new Exception(getDetailMessage(result, "Plan execution failed"));
                } else if (resultStatus == ReportNodeStatus.TECHNICAL_ERROR) {
                    error = new Exception(getDetailMessage(result, "Technical error while executing plan"));
                } else if (resultStatus != ReportNodeStatus.PASSED) {
                    error = new Exception(getDetailMessage(result, "The plan execution returned an unexpected status:" + resultStatus));
                }
                if (error!=null) {
                    runNotifier.fireTestFailure(new Failure(getDescription(),error));
                }
            } else {
                runNotifier.fireTestFailure(new Failure(getDescription(), initializingException));
            }
        } catch (Throwable t) {
            runNotifier.fireTestFailure(new Failure(getDescription(), t));
        } finally {
            runNotifier.fireTestFinished(getDescription());
        }
    }

    protected String getDetailMessage(PlanRunnerResult res, String errorMsg) {
        String executionTree;
        Writer w = new StringWriter();
        try {
            res.printTree(w, true);
            executionTree = w.toString();
        } catch (IOException e) {
            executionTree = "Error while writing tree. See logs for details.";
        }
        return errorMsg + "\nExecution tree is:\n" + executionTree;
    }

    public Plan getPlan() {
        return plan;
    }
}