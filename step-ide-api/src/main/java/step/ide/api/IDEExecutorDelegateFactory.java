package step.ide.api;

import step.core.execution.model.ExecutionParameters;

import java.io.File;

public interface IDEExecutorDelegateFactory {
    IDEExecutorDelegate createIDEExecutorDelegate(File apFolder, ExecutionParameters executionParams);
}
