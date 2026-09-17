package step.ide.api;

import java.nio.file.Path;

public interface IDEDelegator {

    LocalExecutionDelegate delegate(LocalExecutionRequest request);
    void execute(Path apPath, RemoteExecutionRequest request) throws Exception;
    void deploy(Path apPath, RemoteDeploymentRequest request) throws Exception;
}
