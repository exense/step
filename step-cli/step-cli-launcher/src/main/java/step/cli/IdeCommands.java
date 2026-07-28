package step.cli;

import org.apache.commons.lang3.function.Failable;
import org.slf4j.Logger;
import picocli.CommandLine;
import step.cli.parameters.ApExecuteParameters;
import step.core.Constants;
import step.core.execution.model.ExecutionParameters;
import step.ide.LocalIDE;
import step.ide.LocalIDEState;
import step.ide.api.IDEExecutorDelegate;
import step.ide.api.IDEExecutorDelegateFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class IdeCommands {
    private IdeCommands() {
    }

    public static class IdeBaseCommand extends BaseCommand implements IDEExecutorDelegateFactory {
        protected static final Logger logger = StepConsole.log;

        protected LocalIDEState getState() {
            return LocalIDEState.get();
        }

        @CommandLine.Option(names = {"--no-browser"}, defaultValue = "false", description = "Skip launching the browser after starting.")
        public boolean noBrowser;

        @Override
        public Integer call() {
            try {
                validateArguments();
                startBackend();
                afterBackendStart();
                return awaitTermination();
            } catch (CommandLine.ParameterException e) {
                // This is handled by PicoCLI itself, it will result in ExitCode.USAGE (=2)
                throw e;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return CommandLine.ExitCode.SOFTWARE; // (=1)
            }
        }

        protected void validateArguments() throws CommandLine.ParameterException {
            // overridden in subclasses as needed
        }

        protected void startBackend() throws Exception {
            LocalIDEState state = LocalIDEState.get();
            CompletableFuture<Void> awaitStartup = new CompletableFuture<>();
            state.setStartupAwaitFuture(awaitStartup);
            // Note that the start() method is currently invoked synchronously, i.e. it will block
            // unit startup is either complete, or failed. This does not break any functionality,
            // it just renders the timeout handling below useless -- the future will (should!) always
            // be finished (either normally, or exceptionally) by the time start() returns.
            // Not sure if doing it asynchronously (i.e. in a separate thread) has real benefits though.
            new LocalIDE().start();
            // Wait until startup is complete, handling various error scenarios
            long timeoutSeconds = 60;
            try {
                // Wait for the backend to start, with timeout (see above note)
                awaitStartup.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new Exception("Backend failed to start within the " + timeoutSeconds + " second timeout", e);
            } catch (ExecutionException e) {
                throw new Exception("Backend startup failed with an exception", e.getCause());
            } catch (InterruptedException e) {
                // Best practice: restore the interrupted status if we catch an InterruptedException
                Thread.currentThread().interrupt();
                throw new Exception("Thread was interrupted while waiting for backend to start", e);
            }
            // Wire the execution redirection so executions get run in an isolated context
            state.setExecutorDelegateFactory(this);
        }

        protected void afterBackendStart() throws Exception {
            String browserUrl = "http://localhost:4201/"; // FIXME revise
            if (noBrowser) {
                logger.info("The IDE backend started successfully. To access it, please navigate to: {}", browserUrl);
                return;
            }
            openBrowser(browserUrl);
        }

        private static void openBrowser(String url) {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            try {
                if (os.contains("win")) {
                    // Passed as separate arguments to prevent tokenization bugs
                    pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("open", url);
                } else if (os.contains("nix") || os.contains("nux") || os.contains("bsd")) {
                    pb = new ProcessBuilder("xdg-open", url);
                } else {
                    logger.warn("Unable to determine how to launch browser. Please manually navigate to: {}", url);
                    return;
                }
                pb.start();
                logger.info("Your browser should have opened the IDE. If it hasn't, please manually navigate to: {}", url);
            } catch (Exception e) {
                logger.warn("Failed to launch browser: {}. Please manually navigate to: {}", e.getMessage(), url);
            }
        }

        @Override
        public IDEExecutorDelegate createIDEExecutorDelegate(File apFolder, ExecutionParameters executionParams) {
            ApExecuteParameters params = new ApExecuteParameters()
                .setAutomationPackageFile(ApCommand.AbstractApCommand.prepareFile(Failable.call(apFolder::getCanonicalPath), "automation package", true))
                .setAutomationPackageMavenArtifact(null)
                .setLibraryFile(null)
                .setlibraryMavenArtifact(null)
                .setManagedLibraryName(null)
                .setStepProjectName(null)
                .setUserId(null)
                .setAuthToken(null)
                .setExecutionParameters(executionParams.getCustomParameters())
                .setExecutionResultTimeoutS(3600)
                .setWaitForExecution(false)
                .setEnsureExecutionSuccess(false)
                .setIncludePlans(executionParams.getDescription()) // NOTE: the include plans is slightly buggy as it splits plan names by commas (",") -- so don't use plan names with a comma.
                .setExcludePlans(null)
                .setIncludeCategories(null)
                .setExcludeCategories(null)
                .setWrapIntoTestSet(false)
                .setNumberOfThreads(null)
                .setReports(null)
                .setReportOutputDir(Failable.call(() -> Files.createTempDirectory("step-ide-execution-").toFile())); // FIXME clean up tmp dir
            String url = "http://localhost:8080";
            return new ExecuteAutomationPackageTool(url, params);
        }

        private int awaitTermination() {
            // This awaits specific user input
            CompletableFuture<Void> quitCommand = CompletableFuture.runAsync(() -> {
                Scanner scanner = new Scanner(System.in);
                while (scanner.hasNextLine()) {
                    String input = scanner.nextLine().trim().toLowerCase();
                    if (input.equals("q") || input.equals("quit")) {
                        logger.debug("User entered termination command: {}", input);
                        break; // Exiting the loop will complete this future
                    }
                }
            });
            // This will be triggered when the backend is shutdown (e.g. using Ctrl-C, or via REST call)
            CompletableFuture<Void> backendShutdown = new CompletableFuture<>();
            getState().setShutdownAwaitFuture(backendShutdown);

            logger.info("The IDE is running. Type 'quit' (or 'q') to exit. You can also press Ctrl-C");

            try {
                CompletableFuture.anyOf(backendShutdown, quitCommand).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Main thread interrupted.");
                return 1;
            } catch (ExecutionException e) {
                logger.error("Error while waiting for backend shutdown: ", e);
                return 1;
            }
            return 0;
        }
    }

    @CommandLine.Command(name = "ide",
        description = "The CLI interface to launch the local Step IDE",
        version = Constants.STEP_VERSION_STRING,
        mixinStandardHelpOptions = true, usageHelpAutoWidth = true,
        subcommands = {IdeCommands.IdeOpenCommand.class, CommandLine.HelpCommand.class}
    )
    public static class IdeCommand extends IdeBaseCommand {
    }

    @CommandLine.Command(name = "open",
        description = "Opens an Automation Package in the IDE",
        version = Constants.STEP_VERSION_STRING,
        mixinStandardHelpOptions = true, usageHelpAutoWidth = true
    )
    public static class IdeOpenCommand extends IdeBaseCommand {

        @CommandLine.Option(names = {"-d", "--directory"}, defaultValue = ".", description = "The Automation Package directory to use for the operation. Defaults to the current working directory.")
        protected Path apDirectory;

        @CommandLine.ArgGroup(exclusive = false, heading = "%nInitialization Options:%n")
        public InitGroup initGroup;

        public static class InitGroup {
            // required = true here means it is only required if the group is triggered
            @CommandLine.Option(names = {"--init"}, required = true, description = "Initializes an Automation Package")
            public boolean initialize;

            @CommandLine.Option(names = {"--force"}, description = "Forces reinitialization, i.e., overwrites existing AP descriptors. Use with caution! (requires --init)")
            public boolean force;

            @CommandLine.Option(names = {"--name"}, description = "Name of the AP. Defaults to the directory name if not specified. (requires --init)")
            public String name;
        }

        @Override
        protected void validateArguments() throws CommandLine.ParameterException {
            try {
                var ideState = getState();
                if (initGroup == null || !initGroup.initialize) {
                    ideState.validateExistingAutomationPackageDirectory(apDirectory);
                    return;
                }
                // initialization requested
                Path existingDescriptor = ideState.validateInitializableAutomationPackageDirectory(apDirectory);
                if (existingDescriptor != null && !initGroup.force) {
                    throw new IllegalArgumentException("Automation Package descriptor already exists at " + existingDescriptor + ". Use --force to overwrite.");
                }
            } catch (Exception e) {
                throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
            }
        }

        @Override
        protected void afterBackendStart() throws Exception {
            if (initGroup == null || !initGroup.initialize) {
                getState().useExistingAutomationPackageDirectory(apDirectory);
            } else {
                getState().useNewAutomationPackageDirectory(apDirectory, initGroup.name);
            }
            super.afterBackendStart();
        }
    }
}
