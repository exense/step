package step.functions.execution;

/**
 * Interface used for intercepting token creation and disposal.
 * <p>
 * On returning a token (i.e., when the implementation has finished using the token), no complicated operations should be performed,
 * and are must be taken not to (inadvertently) throw Exceptions.
 * </p><p>
 * On getting a token, the method may (knowingly) throw an exception,
 * in which case the entire token retrieval process is aborted, and thus the execution
 * will not take place.
 * </p>
 */
public interface TokenLifecycleInterceptor {
    void onReturnTokenHandle(String tokenHandleId);

    void onGetTokenHandle(String tokenHandleId) throws Exception;
}
