package step.ide.api;

import step.core.execution.model.ExecutionParameters;

import java.io.File;
import java.util.List;

public interface IDEExecutorDelegateFactory {

    IDEExecutorDelegate createDelegate(IDEExecutionRequest request);

    /**
     * @deprecated use {@link #createDelegate(IDEExecutionRequest)}. Kept as a bridge for implementors that still pass
     * the plan name through the execution description.
     */
    @Deprecated
    default IDEExecutorDelegate createIDEExecutorDelegate(File apFolder, ExecutionParameters executionParams) {
        String description = executionParams.getDescription();
        List<String> includedPlanNames = (description == null || description.isBlank()) ? List.of() : List.of(description);
        return createDelegate(new IDEExecutionRequest(apFolder.toPath(), executionParams, includedPlanNames));
    }
}
