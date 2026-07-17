package step.automation.packages;

import step.controller.services.async.AsyncTaskStatus;

/**
 * Concrete, named specialization of {@link AsyncTaskStatus} carrying an {@link AutomationPackageUpdateResult}.
 * <p>
 * This type exists solely so that the OpenAPI schema for the asynchronous automation package deployment response
 * is generated with the name {@code AsyncTaskStatusAutomationPackageUpdateResult} (with a concrete {@code result}
 * type), consistently with the other {@code AsyncTaskStatus<...>} endpoints (e.g. {@code AsyncTaskStatusResource},
 * {@code AsyncTaskStatusObject}).
 */
public class AsyncTaskStatusAutomationPackageUpdateResult extends AsyncTaskStatus<AutomationPackageUpdateResult> {
}
