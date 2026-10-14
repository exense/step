package step.studio.api;

import java.util.concurrent.CompletableFuture;

public interface LocalExecutionDelegate {
    void executePackageAndFillExecutionId(CompletableFuture<String> singleExecutionIdFuture) throws Exception;
}
