package step.controller.services.async;

import java.util.Set;

public class AsyncTaskHandle {

    private final AsyncTaskStatus<?> asyncTaskStatus;

    public AsyncTaskHandle(AsyncTaskStatus<?> asyncTaskStatus) {
        this.asyncTaskStatus = asyncTaskStatus;
    }

    public void setWarnings(Set<String> warnings) {
        asyncTaskStatus.setWarnings(warnings);
    }

    public void updateProgress(float progress) {
        asyncTaskStatus.setProgress(progress);
    }
}
