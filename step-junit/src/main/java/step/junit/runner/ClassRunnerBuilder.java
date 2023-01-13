package step.junit.runner;

import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;
import step.core.execution.ExecutionEngine;

import java.util.HashMap;

public class ClassRunnerBuilder extends RunnerBuilder {

    private final StepClassParser classParser = new StepClassParser(false);

    @Override
    public Runner runnerForClass(Class<?> klass) throws Throwable {
        return new StepClassRunner(klass,classParser, new HashMap<>(), ExecutionEngine.builder().build());
    }
}
