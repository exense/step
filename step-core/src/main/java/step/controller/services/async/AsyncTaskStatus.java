package step.controller.services.async;

import java.util.Set;

public class AsyncTaskStatus<T> {

    private String id;
    private volatile boolean ready = false;
    private volatile float progress = 0;
    private Set<String> warnings;
    private String error;
    private T result;

    public AsyncTaskStatus() {
        super();
    }

    public AsyncTaskStatus(String id) {
        super();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public Set<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(Set<String> warnings) {
        this.warnings = warnings;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
