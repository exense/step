package step.functions.execution;

public interface TokenLifecycleInterceptor {
    void onReturnTokenHandle(String tokenHandleId);

    void onGetTokenHandle(String tokenHandleId) throws Exception;
}
