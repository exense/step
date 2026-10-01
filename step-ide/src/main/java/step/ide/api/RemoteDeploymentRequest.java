package step.ide.api;

import java.util.Map;

/**
 * The options of a deployment of the opened automation package to a remote Step controller, mirroring
 * the options of the {@code step ap deploy} command. Every field is optional: a field left null falls
 * back to what is configured in the CLI properties.
 * <p>
 * The automation package itself is not part of the request, it is always the currently opened one.
 *
 * @param library the package library, as a file path, a maven coordinate ({@code mvn:groupId:artifactId:version})
 *                or the name of a managed library ({@code managed:MY_LIBRARY})
 */
public record RemoteDeploymentRequest(StepConnectionInfo connection,
                                      String library,
                                      Boolean async,
                                      String versionName,
                                      String activationExpression,
                                      Boolean forceRefreshOfSnapshots,
                                      Integer deploymentTimeout,
                                      Map<String, String> plansAttributes,
                                      Map<String, String> keywordsAttributes,
                                      Map<String, String> tokenSelectionCriteria,
                                      Boolean executeKeywordsOnController) {

    public static final RemoteDeploymentRequest DEFAULTS =
        new RemoteDeploymentRequest(null, null, null, null, null, null, null, null, null, null, null);
}
