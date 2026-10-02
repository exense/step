package step.ide.api;

/**
 * The connection to a remote Step controller. Every field is optional: a field left null in a request
 * falls back to what is configured in the CLI properties.
 * <p>
 * {@link #tokenConfigured} is only filled when this record describes the configured defaults, so that
 * the client can indicate that an API key is available without the key itself being exposed.
 */
public record StepConnectionInfo(String url, String projectName, String token, String stepUser,
                                 Boolean tokenConfigured) {

    public StepConnectionInfo(String url, String projectName, String token, String stepUser) {
        this(url, projectName, token, stepUser, null);
    }

    /**
     * @param port the port of the controller running on the local host
     * @return the connection to the local controller
     */
    public static StepConnectionInfo local(int port) {
        return new StepConnectionInfo("http://localhost:" + port, null, null, null, null);
    }

    /**
     * Returns a copy without the API key, reporting through {@link #tokenConfigured} whether one is set.
     */
    public StepConnectionInfo withoutToken() {
        return new StepConnectionInfo(url, projectName, null, stepUser, token != null && !token.isBlank());
    }
}
