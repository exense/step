package step.ide.api;

import java.util.concurrent.CompletableFuture;

public interface IDEExecutorDelegate {
    void executeStuffForIDE(CompletableFuture<String> singleExecutionIdFuture) throws Exception;
}
