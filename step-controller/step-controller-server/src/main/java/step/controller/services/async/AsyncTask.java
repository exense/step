package step.controller.services.async;

@FunctionalInterface
public interface AsyncTask<T extends Object> {
    T apply(AsyncTaskManager.AsyncTaskHandle t) throws Exception;
}
