package step.cli;

import picocli.CommandLine;
import step.agents.provisioning.local.LocalAgentProvisioningConfiguration;
import step.automation.packages.AutomationPackageUpdateResult;
import step.cli.parameters.ApDeployParameters;
import step.cli.parameters.ApExecuteParameters;
import step.ide.api.RemoteDefaults;
import step.ide.api.RemoteDeploymentRequest;
import step.ide.api.RemoteExecution;
import step.ide.api.RemoteExecutionRequest;
import step.ide.api.StepConnectionInfo;
import step.ide.exceptions.InvalidRequestException;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Deploys and executes the automation package opened in the IDE on a remote Step controller, the way the
 * {@code step ap deploy} and {@code step ap execute} commands do.
 * <p>
 * Both operations run the real picocli commands: the command is first populated from the CLI properties, then
 * the options the IDE request sets are applied on top of it, and finally the command performs the operation.
 * The IDE therefore supports the same options, property keys and validations as the CLI, without a second
 * mapping that could drift from the one the commands already have.
 */
class IdeRemoteDelegate {

    private static final String DEPLOY_COMMAND = "deploy";
    private static final String EXECUTE_COMMAND = "execute";

    private final CommandLine.IDefaultValueProvider defaultValueProvider;

    IdeRemoteDelegate(CommandLine.IDefaultValueProvider defaultValueProvider) {
        this.defaultValueProvider = defaultValueProvider;
    }

    AutomationPackageUpdateResult deploy(Path apPath, RemoteDeploymentRequest request) {
        IdeDeployCommand command = prepareDeploy(apPath, request);
        try {
            run(command::handleApDeployCommand);
            return command.updateResult;
        } finally {
            command.deleteTemporaryArchives();
        }
    }

    List<RemoteExecution> execute(Path apPath, RemoteExecutionRequest request) {
        IdeExecuteCommand command = prepareExecute(apPath, request);
        try {
            run(command::handleApRemoteExecuteCommand);
            return command.startedExecutions.stream()
                .map(started -> new RemoteExecution(started.id(), started.description(),
                    executionUrl(command.stepUrl, started.id())))
                .toList();
        } finally {
            command.deleteTemporaryArchives();
        }
    }

    /**
     * Builds the deployment command, starting from the configured options and applying the ones the request sets.
     */
    IdeDeployCommand prepareDeploy(Path apPath, RemoteDeploymentRequest request) {
        IdeDeployCommand command = configure(new IdeDeployCommand(), DEPLOY_COMMAND);
        applyConnection(command, request.connection());

        if (request.library() != null) {
            command.library = request.library();
        }
        if (request.async() != null) {
            command.async = request.async();
        }
        if (request.versionName() != null) {
            command.versionName = request.versionName();
        }
        if (request.activationExpression() != null) {
            command.activationExpression = request.activationExpression();
        }
        if (request.forceRefreshOfSnapshots() != null) {
            command.forceRefreshOfSnapshots = request.forceRefreshOfSnapshots();
        }
        if (request.deploymentTimeout() != null) {
            command.deploymentTimeout = request.deploymentTimeout();
        }
        if (request.plansAttributes() != null) {
            command.plansAttributes = request.plansAttributes();
        }
        if (request.keywordsAttributes() != null) {
            command.keywordsAttributes = request.keywordsAttributes();
        }
        if (request.tokenSelectionCriteria() != null) {
            command.tokenSelectionCriteria = request.tokenSelectionCriteria();
        }
        if (request.executeKeywordsOnController() != null) {
            command.executeKeywordsOnController = request.executeKeywordsOnController();
        }
        command.apFile = apPath.toAbsolutePath().toString();
        return command;
    }

    /**
     * Builds the execution command, starting from the configured options and applying the ones the request sets.
     */
    IdeExecuteCommand prepareExecute(Path apPath, RemoteExecutionRequest request) {
        IdeExecuteCommand command = configure(new IdeExecuteCommand(), EXECUTE_COMMAND);
        applyConnection(command, request.connection());

        if (request.library() != null) {
            command.library = request.library();
        }
        if (request.includePlans() != null) {
            command.includePlans = request.includePlans();
        }
        if (request.excludePlans() != null) {
            command.excludePlans = request.excludePlans();
        }
        if (request.includeCategories() != null) {
            command.includeCategories = request.includeCategories();
        }
        if (request.excludeCategories() != null) {
            command.excludeCategories = request.excludeCategories();
        }
        if (request.wrapIntoTestSet() != null) {
            command.wrapIntoTestSet = request.wrapIntoTestSet();
        }
        if (request.numberOfThreads() != null) {
            command.numberOfThreads = request.numberOfThreads();
        }
        if (request.executionParameters() != null) {
            command.executionParameters = request.executionParameters();
        }
        command.apFile = apPath.toAbsolutePath().toString();
        // The IDE starts the execution and reports its id, it never waits for the execution to complete, and it
        // has no working directory to write execution reports to.
        command.local = false;
        command.async = true;
        command.reportType = null;
        return command;
    }

    RemoteDefaults remoteDefaults() {
        IdeDeployCommand deploy = configure(new IdeDeployCommand(), DEPLOY_COMMAND);
        IdeExecuteCommand execute = configure(new IdeExecuteCommand(), EXECUTE_COMMAND);
        return new RemoteDefaults(
            new RemoteDeploymentRequest(
                connectionOf(deploy),
                deploy.library,
                deploy.async,
                deploy.versionName,
                deploy.activationExpression,
                deploy.forceRefreshOfSnapshots,
                deploy.deploymentTimeout,
                deploy.plansAttributes,
                deploy.keywordsAttributes,
                deploy.tokenSelectionCriteria,
                deploy.executeKeywordsOnController),
            new RemoteExecutionRequest(
                connectionOf(execute),
                execute.library,
                execute.includePlans,
                execute.excludePlans,
                execute.includeCategories,
                execute.excludeCategories,
                execute.wrapIntoTestSet,
                execute.numberOfThreads,
                execute.executionParameters));
    }

    LocalAgentProvisioningConfiguration localAgentConfiguration() {
        return configure(new ApCommand.ApExecuteCommand(), EXECUTE_COMMAND).buildLocalAgentConfiguration();
    }

    private static String executionUrl(String stepUrl, String executionId) {
        if (stepUrl == null || executionId == null) {
            return null;
        }
        return (stepUrl.endsWith("/") ? stepUrl : stepUrl + "/") + "#/executions/" + executionId;
    }

    private static StepConnectionInfo connectionOf(StepConsole.AbstractStepCommand command) {
        return new StepConnectionInfo(command.stepUrl, command.stepProjectName, command.authToken, command.stepUser)
            .withoutToken();
    }

    private static void applyConnection(StepConsole.AbstractStepCommand command, StepConnectionInfo connection) {
        if (connection == null) {
            return;
        }
        if (connection.url() != null) {
            command.stepUrl = connection.url();
        }
        if (connection.projectName() != null) {
            command.stepProjectName = connection.projectName();
        }
        if (connection.token() != null) {
            command.authToken = connection.token();
        }
        if (connection.stepUser() != null) {
            command.stepUser = connection.stepUser();
        }
    }

    /**
     * Instantiates the given command within the real {@code step ap <name>} command tree, so that picocli
     * populates it from the CLI properties exactly as it would for a command line invocation, resolving both
     * the plain and the command-qualified property keys.
     */
    private <T> T configure(T command, String name) {
        CommandLine.IFactory factory = new SubstitutingFactory(command);
        CommandLine root = new CommandLine(new StepConsole(), factory)
            .setCaseInsensitiveEnumValuesAllowed(true);
        if (defaultValueProvider != null) {
            root.setDefaultValueProvider(defaultValueProvider);
        }
        root.parseArgs(ApCommand.COMMAND_NAME, name);
        return command;
    }

    /**
     * Translates the failures the commands report for a misconfigured or incomplete request, so that they reach
     * the client as a bad request instead of an internal error.
     */
    private static void run(Runnable operation) {
        try {
            operation.run();
        } catch (CommandLine.ParameterException e) {
            throw new InvalidRequestException(e.getMessage(), e);
        }
    }

    /**
     * Returns the command the IDE prepared wherever picocli would create a new one, leaving the rest of the
     * command tree to the default factory.
     */
    private record SubstitutingFactory(Object substitute) implements CommandLine.IFactory {
        @Override
        public <K> K create(Class<K> cls) throws Exception {
            return cls.isInstance(substitute) ? cls.cast(substitute) : CommandLine.defaultFactory().create(cls);
        }
    }

    /**
     * Keeps track of the archives the command zips the opened automation package and its library into, so that
     * they can be deleted once the operation is over. Unlike the CLI, the IDE is a long running process, and
     * would otherwise accumulate them until it is shut down.
     */
    private interface TemporaryArchiveTracker {

        List<File> temporaryArchives();

        default File track(File archive, File source) {
            // Only a directory is zipped into a temporary archive, a file is passed on as it is.
            if (source != null && source.isDirectory()) {
                temporaryArchives().add(archive);
            }
            return archive;
        }

        default void deleteTemporaryArchives() {
            for (File archive : temporaryArchives()) {
                // Each archive sits alone in a temporary directory of its own, which goes away with it.
                archive.delete();
                archive.getParentFile().delete();
            }
            temporaryArchives().clear();
        }
    }

    @CommandLine.Command(name = DEPLOY_COMMAND)
    static class IdeDeployCommand extends ApCommand.ApDeployCommand implements TemporaryArchiveTracker {

        private final List<File> temporaryArchives = new ArrayList<>();
        private AutomationPackageUpdateResult updateResult;

        @Override
        public List<File> temporaryArchives() {
            return temporaryArchives;
        }

        @Override
        protected File prepareApFile(String param) {
            return track(super.prepareApFile(param), param == null ? null : new File(param));
        }

        @Override
        protected File preparePackageLibraryFile(String param) {
            return track(super.preparePackageLibraryFile(param), param == null ? null : new File(param));
        }

        @Override
        protected void executeTool(String stepUrl, ApDeployParameters params) {
            updateResult = new DeployAutomationPackageTool(stepUrl, params).execute();
        }
    }

    @CommandLine.Command(name = EXECUTE_COMMAND)
    static class IdeExecuteCommand extends ApCommand.ApExecuteCommand implements TemporaryArchiveTracker {

        private final List<File> temporaryArchives = new ArrayList<>();
        private List<ExecuteAutomationPackageTool.StartedExecution> startedExecutions = List.of();

        @Override
        public List<File> temporaryArchives() {
            return temporaryArchives;
        }

        @Override
        protected File prepareApFile(String param) {
            return track(super.prepareApFile(param), param == null ? null : new File(param));
        }

        @Override
        protected File preparePackageLibraryFile(String param) {
            return track(super.preparePackageLibraryFile(param), param == null ? null : new File(param));
        }

        @Override
        protected void executeRemotely(String stepUrl, ApExecuteParameters params) {
            startedExecutions = new ExecuteAutomationPackageTool(stepUrl, params).execute();
        }
    }
}
