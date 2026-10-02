package step.core.execution;

import step.core.execution.model.ExecutionParameters;

public interface ExecutionDiversion {
    String divertExecution(ExecutionParameters executionParams);
}
