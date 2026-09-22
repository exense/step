package step.ide.api;

import java.util.List;
import java.util.Map;

/**
 * The options of an execution of the opened automation package on a remote Step controller, mirroring
 * the remote options of the {@code step ap execute} command. Every field is optional: a field left null
 * falls back to what is configured in the CLI properties.
 * <p>
 * The automation package itself is not part of the request, it is always the currently opened one. The
 * execution is always started asynchronously, so the options controlling whether the CLI waits for the
 * execution to complete have no equivalent here.
 *
 * @param library the package library, as a file path, a maven coordinate ({@code mvn:groupId:artifactId:version})
 *                or the name of a managed library ({@code managed:MY_LIBRARY})
 */
public record RemoteExecutionRequest(StepConnectionInfo connection,
                                     String library,
                                     List<String> includePlans,
                                     List<String> excludePlans,
                                     List<String> includeCategories,
                                     List<String> excludeCategories,
                                     Boolean wrapIntoTestSet,
                                     Integer numberOfThreads,
                                     Map<String, String> executionParameters) {

    public static final RemoteExecutionRequest DEFAULTS =
        new RemoteExecutionRequest(null, null, null, null, null, null, null, null, null);
}
