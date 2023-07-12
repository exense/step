package step.engine.execution;

import step.core.execution.ExecutionContext;

import java.util.List;

public interface ExecutionVetoer {
    List<ExecutionVeto> getExecutionVetoes(ExecutionContext context);
}
