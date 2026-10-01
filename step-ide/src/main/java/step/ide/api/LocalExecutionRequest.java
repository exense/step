package step.ide.api;

import step.core.execution.model.ExecutionParameters;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Describes a local automation package execution launched by the IDE.
 *
 * @param automationPackage   the automation package to execute. This is the <b>directory</b> of the currently opened
 *                            package for regular executions, but may also be an external packaged archive
 * @param executionParameters the execution parameters, notably the custom parameters handed to the package
 * @param includedPlanNames   the names of the plans to execute; an empty list executes all plans of the package
 */
public record LocalExecutionRequest(Path automationPackage,
                                    ExecutionParameters executionParameters,
                                    List<String> includedPlanNames
) {

    public LocalExecutionRequest {
        Objects.requireNonNull(automationPackage, "automationPackage must not be null");
        Objects.requireNonNull(executionParameters, "executionParameters must not be null");
        includedPlanNames = (includedPlanNames == null) ? List.of() : List.copyOf(includedPlanNames);
    }
}
