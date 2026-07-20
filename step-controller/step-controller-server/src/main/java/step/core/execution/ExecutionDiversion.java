package step.core.execution;

import step.core.execution.model.ExecutionParameters;

// TODO SED-4429 Rethink and potentially refactor this entire construct
public interface ExecutionDiversion {
    String divertExecution(ExecutionParameters executionParams);
}
