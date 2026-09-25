package step.ide.api;

import java.util.concurrent.CompletableFuture;

public interface IDEExecutorDelegate {
    void executePackageAndFillExecutionId(CompletableFuture<String> singleExecutionIdFuture) throws Exception;
}
