package step.ide.api;

import java.util.concurrent.CompletableFuture;

public interface LocalExecutionDelegate {
    void executePackageAndFillExecutionId(CompletableFuture<String> singleExecutionIdFuture) throws Exception;
}
