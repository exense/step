package step.ide.api;

/**
 * The options the remote deployment and execution requests fall back to, as configured in the CLI
 * properties. Both are given in the shape of the corresponding request, so that a client can display
 * them and only has to send back the options it actually overrides.
 */
public record RemoteDefaults(RemoteDeploymentRequest deploy, RemoteExecutionRequest execute) {
}
