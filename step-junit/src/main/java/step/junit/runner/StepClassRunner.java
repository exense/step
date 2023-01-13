package step.junit.runner;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import step.core.execution.ExecutionEngine;
import java.util.List;
import java.util.Map;

public class StepClassRunner extends Runner {

    private final Class klass;
    private final List<Runner> runners;

    public StepClassRunner(Class klass, StepClassParser parser, Map<String, String> executionParameters, ExecutionEngine executionEngine) {
        super();
        this.klass = klass;
        this.runners = parser.createRunnersForClass(klass, executionEngine, executionParameters);
    }

    @Override
    public Description getDescription() {
        return Description.createSuiteDescription(klass);
    }

    @Override
    public void run(RunNotifier runNotifier) {
        runNotifier.fireTestSuiteStarted(getDescription());
        for (Runner runner: runners) {
            runner.run(runNotifier);
        }
        runNotifier.fireTestSuiteFinished(getDescription());
    }
}