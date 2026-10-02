package step.ide.api;

/**
 * An execution started on the remote Step controller, with a link to follow it there.
 *
 * @param description the description the controller gave the execution, which is the plan name when the plans of
 *                    the automation package are executed one per plan rather than wrapped into a single test set
 */
public record RemoteExecution(String executionId, String description, String executionUrl) {
}
