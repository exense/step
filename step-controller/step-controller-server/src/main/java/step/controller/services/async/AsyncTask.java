package step.controller.services.async;

@FunctionalInterface
public interface AsyncTask<T> {
    T apply(AsyncTaskHandle t) throws Exception;
}
