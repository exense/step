package step.cli;

import org.apache.commons.lang3.function.Failable;
import org.slf4j.Logger;
import picocli.CommandLine;
import step.cli.parameters.ApExecuteParameters;
import step.core.Constants;
import step.core.execution.model.ExecutionParameters;
import step.ide.LocalIDEState;
import step.ide.api.IDEExecutorDelegate;
import step.ide.api.IDEExecutorDelegateFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;


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
        public Integer call() throws Exception {
            validateArguments();
            startBackend();
            afterBackendStart();
            System.err.println("TODO SED-4429: PROPER TERMINATION HANDLING; FOR NOW, JUST STOP THE PROCESS");
            Thread.sleep(Long.MAX_VALUE);
            return 0;
        }

        protected void validateArguments() {
        }

        protected void startBackend() throws Exception {
            LocalIDEState.get().setExecutorDelegateFactory(this);
            step.ide.LocalIDE.main(new String[]{});
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
        public IDEExecutorDelegate createDelegate(File apFolder, ExecutionParameters executionParams) {
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
        protected void validateArguments() {
            try {
                var ideState = getState();
                if (initGroup == null || !initGroup.initialize) {
                    ideState.validateExistingAutomationPackageDirectory(apDirectory);
                    return;
                }
                // initialization requested
                Path existingDescriptor = getState().validateInitializableAutomationPackageDirectory(apDirectory);
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
