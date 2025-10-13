/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.cli;

import ch.exense.commons.io.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import step.automation.packages.AutomationPackageArchive;
import step.automation.packages.AutomationPackageFromFolderProvider;
import step.automation.packages.AutomationPackageReadingException;
import step.cli.apignore.ApIgnoreFileFilter;
import step.client.controller.ControllerServicesClient;
import step.client.credentials.ControllerCredentials;
import step.core.Constants;
import step.core.Version;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Command(name = "step",
        mixinStandardHelpOptions = true,
        version = Constants.STEP_API_VERSION_STRING,
        description = "The command-line interface (CLI) to interact with Step",
        usageHelpAutoWidth = true
)
public class StepConsole implements Callable<Integer> {

    public static final String REQUIRED_ERR_MESSAGE = "Illegal parameters. One of the following options is required: '%s'";

    private static final String AP_IGNORE_NAME = ".apignore";

    private static final Logger log = LoggerFactory.getLogger(StepConsole.class);

    @Override
    public Integer call() throws Exception {
        // call help by default
        return addStepSubcommands(new CommandLine(new StepConsole()), ApCommand.ApDeployCommand::new, ApCommand.ApExecuteCommand::new)
                .setExecutionExceptionHandler(new StepExecutionExceptionHandler())
                .execute("help");
    }

    public static abstract class AbstractStepCommand implements Callable<Integer> {

        public static final String STEP_URL_SHORT = "-u";
        public static final String STEP_URL = "--stepUrl";
        public static final String PROJECT_NAME = "--projectName";
        public static final String TOKEN = "--token";
        public static final String STEP_USER = "--stepUser";
        public static final String VERBOSE = "--verbose";
        public static final String CONFIG = "-c";
        public static final String LOCAL = "--local";
        public static final String FORCE = "--force";
        public static final String ALLOW_UPDATE_OF_OTHER_PACKAGES = "--allowUpdateOfOtherPackages";

        @CommandLine.Spec
        protected CommandLine.Model.CommandSpec spec;

        @Option(names = {CONFIG}, paramLabel = "<configFile>", description = "Optional configuration file(s) containing CLI options (ex: projectName=Common)")
        protected List<String> config;

        @Option(names = {STEP_URL_SHORT, STEP_URL}, description = "The URL of the remote Step server")
        protected String stepUrl;

        @Option(names = {PROJECT_NAME}, description = "The target project name in Step")
        protected String stepProjectName;

        @Option(names = {TOKEN}, paramLabel = "<API key>", description = "The API key (token) to authenticate to the remote Step server")
        protected String authToken;

        @Option(names = {STEP_USER}, description = "To execute on behalf of the provided user")
        protected String stepUser;

        @Option(names = {VERBOSE}, defaultValue = "false", description = "Verbose mode: prints the applied configuration")
        protected boolean verbose;

        @Option(names = {FORCE}, defaultValue = "false", description = "To force execution in case of uncritical errors")
        protected boolean force;

        @Option(names = {ALLOW_UPDATE_OF_OTHER_PACKAGES}, defaultValue = "false",
                description = "To force update of another automation packages in case they reference the same maven artifact")
        protected boolean allowUpdateOfOtherPackages;

        protected String getStepProjectName() {
            return stepProjectName;
        }

        protected String getAuthToken() {
            return authToken;
        }

        public void checkRequiredParam(CommandLine.Model.CommandSpec spec, String value, String... optionLabels) {
            if (value == null || value.isEmpty()) {
                String optionsList = String.join(",", optionLabels);
                throw new CommandLine.ParameterException(spec.commandLine(), String.format(REQUIRED_ERR_MESSAGE, optionsList));
            }
        }

        public void checkEeOptionsConsistency(CommandLine.Model.CommandSpec spec) {
            // The auth token for Step EE and the project name (for EE) must be used together
            if (getAuthToken() != null && !getAuthToken().isEmpty()) {
                checkRequiredParam(spec, getStepProjectName(), PROJECT_NAME);
            }

            if (getStepProjectName() != null && !getStepProjectName().isEmpty()) {
                checkRequiredParam(spec, getAuthToken(), TOKEN);
            }
        }

        public void printConfigIfRequired() {
            StepDefaultValuesProvider defaultValuesProvider = getStepDefaultValuesProvider();
            if (verbose && defaultValuesProvider != null) {
                defaultValuesProvider.printAppliedConfig();
            }
        }

        protected StepDefaultValuesProvider getStepDefaultValuesProvider() {
            if (spec.defaultValueProvider() instanceof StepDefaultValuesProvider) {
                return (StepDefaultValuesProvider) spec.defaultValueProvider();
            } else {
                return null;
            }
        }

        protected MavenArtifactIdentifier getMavenArtifact(String apFile) {
            if (apFile != null && apFile.startsWith("mvn:")) {
                return MavenArtifactIdentifier.fromShortString(apFile);
            } else {
                return null;
            }
        }

        @Override
        public Integer call() throws Exception {
            printConfigIfRequired();
            return 0;
        }
    }

    @Command(name = ApCommand.AP_COMMAND,
            mixinStandardHelpOptions = true,
            version = Constants.STEP_API_VERSION_STRING,
            description = "The CLI interface to manage automation packages in Step",
            usageHelpAutoWidth = true
    )
    public static class ApCommand implements Callable<Integer> {

        public static final String AP_COMMAND = "ap";

        public static abstract class AbstractApCommand extends AbstractStepCommand {

            @Option(names = {"-p", "--package"}, paramLabel = "<AutomationPackage>", description = "The path to the automation-package.yaml file or to the folder or the archive containing it. Maven coordinate are supported.")
            protected String apFile;

            @Option(names = {"--packageLibrary"}, paramLabel = "<PackageLibrary>", description = "The path or maven coordinate to the archive file containing the Package library for the automation package. This Library is used a keyword library when applicable.")
            protected String packageLibrary;

            /**
             * If the param points to the folder, prepares the zipped AP file with .stz extension.
             * Otherwise, if the param is a simple file, just returns this file
             *
             * @param param the source of AP
             */
            protected File prepareFile(String param, String entityNameForLog, boolean checkApFolder) {
                try {
                    File file = null;
                    if (param == null) {
                        // use the current folder by default
                        file = new File(new File("").getAbsolutePath());
                    } else {
                        file = new File(param);
                    }
                    log.info("The {} is {}", entityNameForLog, file.getAbsolutePath());

                    if (file.isDirectory()) {
                        // check if the folder is AP (contains the yaml descriptor)
                        if(checkApFolder) {
                            checkApFolder(file);
                        }

                        Function<File, Boolean> fileFilter = null;
                        File apIgnoreFile = new File(file, AP_IGNORE_NAME);
                        if (apIgnoreFile.exists()) {
                            ApIgnoreFileFilter gitIgnore = new ApIgnoreFileFilter(file.toPath(), apIgnoreFile.toPath());
                            fileFilter = file1 -> !file1.getName().equals(AP_IGNORE_NAME) && gitIgnore.accept(file1.toPath());
                        }

                        File tempDirectory = Files.createTempDirectory("stepcli").toFile();
                        tempDirectory.deleteOnExit();
                        File tempFile = new File(tempDirectory, file.getName() + ".stz");
                        tempFile.deleteOnExit();
                        log.info("Preparing AP archive: {}", tempFile.getAbsolutePath());
                        FileHelper.zip(file, tempFile, fileFilter);
                        return tempFile;
                    } else {
                        return file;
                    }
                } catch (IOException ex) {
                    throw new StepCliExecutionException("Unable to prepare automation package file", ex);
                }
            }

            protected File prepareApFile(String param) {
                return prepareFile(param, "automation package", true);
            }

            protected File prepareKPackageLibraryFile(String param) {
                return prepareFile(param, "package library", false);
            }

            private void checkApFolder(File param) throws IOException {
                // package library is not required here, because we only need to check the automation package descriptor
                try (AutomationPackageFromFolderProvider apProvider = new AutomationPackageFromFolderProvider(param, null);
                     AutomationPackageArchive apArchive = apProvider.getAutomationPackageArchive()) {
                    if (!apArchive.hasAutomationPackageDescriptor()) {
                        throw new StepCliExecutionException("The AP folder " + param.getAbsolutePath() + " doesn't contain the AP descriptor file");
                    }
                } catch (AutomationPackageReadingException e) {
                    throw new StepCliExecutionException("Unable to read automation package from folder " + param.getAbsolutePath(), e);

                }
            }

            protected ControllerServicesClient createControllerServicesClient() {
                return new ControllerServicesClient(getControllerCredentials());
            }

            protected ControllerCredentials getControllerCredentials() {
                String authToken = getAuthToken();
                return new ControllerCredentials(stepUrl, authToken == null || authToken.isEmpty() ? null : authToken);
            }

            protected void checkStepControllerVersion() {
                try {
                    new ControllerVersionValidator(createControllerServicesClient()).validateVersions(getVersion());
                } catch (ControllerVersionValidator.ValidationException e) {
                    if (e.getResult().getStatus() == ControllerVersionValidator.Status.MINOR_MISMATCH) {
                        String warn = "The CLI version (" + e.getResult().getClientVersion() + ") does not exactly match the server version (" + e.getResult().getServerVersion() + "), but they are considered compatible. It's recommended to use matching versions.";
                        log.warn(warn);
                    } else {
                        String err = "Version mismatch. The server version (" + e.getResult().getServerVersion() + ") is incompatible with the current CLI version (" + e.getResult().getClientVersion() + "). Please ensure both the CLI and server are running compatible versions.";
                        if (!force) {
                            err += " You can use the " + FORCE + " option to ignore this validation.";
                            throw new StepCliExecutionException(err, e);
                        } else {
                            log.warn(err);
                        }
                    }
                }
            }

            protected Version getVersion() {
                return Constants.STEP_API_VERSION;
            }
        }

        @Command(name = "deploy",
                mixinStandardHelpOptions = true,
                version = Constants.STEP_API_VERSION_STRING,
                description = "The CLI interface to deploy automation packages in Step",
                usageHelpAutoWidth = true,
                subcommands = {CommandLine.HelpCommand.class})
        public static class ApDeployCommand extends AbstractApCommand {

            public static final String AP_VERSION = "--apVersion";

            @Option(names = {"--async"}, defaultValue = "false", showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
                    description = "Whether to waits for the deployment to complete")
            protected boolean async;

            @Option(names = {AP_VERSION}, description = "Optionally set the version of this automation package. This allows to deploy and use multiple versions of the same package on Step")
            protected String apVersion;

            @Option(names = {"--activationExpr"}, description = "When deploying multiple versions of the same package (see \"apVersion\"), the expression is used to select the proper versions during the execution of plans. Example: \"env == PROD\"")
            protected String activationExpr;

            @Override
            public Integer call() throws Exception {
                super.call();
                handleApDeployCommand();
                return 0;
            }

            public void checkStepUrlRequired() {
                checkRequiredParam(spec, stepUrl, STEP_URL_SHORT, STEP_URL);
            }

            protected void handleApDeployCommand() {
                checkStepUrlRequired();
                checkEeOptionsConsistency(spec);
                checkStepControllerVersion();
                MavenArtifactIdentifier apMavenArtifact = getMavenArtifact(apFile);
                MavenArtifactIdentifier packageLibraryMavenArtifact = getMavenArtifact(packageLibrary);

                DeployAutomationPackageTool.Params params = new DeployAutomationPackageTool.Params()
                        .setAutomationPackageMavenArtifact(apMavenArtifact)
                        .setAutomationPackageFile(apMavenArtifact != null ? null : prepareApFile(apFile))
                        .setStepProjectName(getStepProjectName())
                        .setAuthToken(getAuthToken())
                        .setAsync(async)
                        .setallowUpdateOfOtherPackages(allowUpdateOfOtherPackages)
                        .setApVersion(apVersion)
                        .setActivationExpression(activationExpr)
                        .setPackageLibraryMavenArtifact(packageLibraryMavenArtifact)
                        .setPackageLibraryFile(packageLibraryMavenArtifact != null || packageLibrary == null || packageLibrary.isEmpty() ? null : prepareKPackageLibraryFile(packageLibrary));
                executeTool(stepUrl, params);
            }

            // for tests
            protected void executeTool(final String stepUrl, DeployAutomationPackageTool.Params params) {
                new DeployAutomationPackageTool(stepUrl, params).execute();
            }
        }

        @Command(name = "execute",
                mixinStandardHelpOptions = true,
                version = "step.ap.execute 1.0",
                description = "The CLI interface to execute automation packages in Step",
                usageHelpAutoWidth = true,
                subcommands = {CommandLine.HelpCommand.class})
        public static class ApExecuteCommand extends AbstractApCommand implements Callable<Integer> {

            public static final String EP_DESCRIPTION_KEY = "executionParameters";

            @Option(names = {"--executionTimeoutS"}, defaultValue = "3600", description = "Maximum time in seconds to wait for executions completeness")
            protected Integer executionTimeoutS;

            @Option(names = {"--async"}, defaultValue = "false", showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
                    description = "Whether to wait for execution completeness")
            protected boolean async;

            @Option(names = {"--includePlans"}, description = "The comma separated list of plans to be executed")
            protected String includePlans;

            @Option(names = {"--excludePlans"}, description = "The comma separated list of plans to be excluded from execution")
            protected String excludePlans;

            @Option(names = {"--includeCategories"}, description = "The comma separated list of categories to be executed")
            protected String includeCategories;

            @Option(names = {"--excludeCategories"}, description = "The comma separated list of categories to be excluded from execution")
            protected String excludeCategories;

            @Option(names = {LOCAL}, defaultValue = "false", description = "To execute the Automation Package locally ", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
            protected boolean local;

            @Option(names = {"--wrapIntoTestSet"}, defaultValue = "false", description = "To wrap all executed plans into the single test set", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
            protected boolean wrapIntoTestSet;

            @Option(names = {"--numberOfThreads"}, description = "Max number of threads to be used for execution in case of wrapped test set")
            protected Integer numberOfThreads;

            @Option(names = {"--reportType"}, description = "The type of execution report to be generated and stored locally. Supported report types: junit, aggregated. Also (optional) you can specify the output destination: --reportType=junit;output=file,stdout")
            protected List<String> reportType;

            @Option(names = {"--reportDir"}, description = "The local folder to store generated execution reports", defaultValue = "reports")
            protected File reportDir;

            @Option(descriptionKey = EP_DESCRIPTION_KEY, names = {"-ep", "--executionParameters"}, description = "Set execution parameters for local and remote executions ", split = "\\|", splitSynopsisLabel = "|")
            protected Map<String, String> executionParameters;

            @Override
            public Integer call() throws Exception {
                super.call();

                // The tricky way to take default values for execution parameters
                // We run the command without any arguments, but with pre-configured default values provider,
                // which will take default values from .properties files specified in original command.
                // After that we can look up the executionParameters in ApExecuteCommand and add them to non-default in our current command
                StepDefaultValuesProvider customDefaultValuesProvider = getStepDefaultValuesProvider();
                if (this.executionParameters == null) {
                    this.executionParameters = new HashMap<>();
                }
                if (customDefaultValuesProvider != null) {
                    ApExecuteCommand defaultExecutionParametersLookup = new ApExecuteCommand();
                    CommandLine clForLookup = new CommandLine(defaultExecutionParametersLookup).setDefaultValueProvider(customDefaultValuesProvider);
                    try {
                        clForLookup.parseArgs();
                    } catch (CommandLine.ParameterException ex) {
                        try {
                            return clForLookup.getParameterExceptionHandler().handleParseException(ex, new String[]{});
                        } catch (Exception handlerException) {
                            // exception during exception handling
                            throw new RuntimeException("Unexpected exception", ex);
                        }
                    }

                    if (defaultExecutionParametersLookup.executionParameters != null) {
                        // apply default execution parameters from config files
                        for (Map.Entry<String, String> defaultEp : defaultExecutionParametersLookup.executionParameters.entrySet()) {
                            this.executionParameters.putIfAbsent(defaultEp.getKey(), defaultEp.getValue());
                        }
                    }
                }

                log.info("Execute automation package with parameters: {}", executionParameters);
                if (!local) {
                    handleApRemoteExecuteCommand();
                } else {
                    handleApLocalExecuteCommand();
                }
                return 0;
            }

            private void handleApLocalExecuteCommand() {
                File file = prepareApFile(apFile);
                if (file == null) {
                    throw new StepCliExecutionException("AP file is not defined");
                }

                if (reportType != null && !reportType.isEmpty()) {
                    throw new StepCliExecutionException("The report generation is not supported for local execution");
                }

                File kwLibFile = null;
                if (packageLibrary != null && !packageLibrary.isEmpty()) {
                    kwLibFile = prepareKPackageLibraryFile(packageLibrary);
                }

                executeLocally(file, kwLibFile, includePlans, excludePlans, includeCategories, excludeCategories, executionParameters);
            }

            protected void executeLocally(File file, File kwLibFile, String includePlans, String excludePlans, String includeCategories,
                                          String excludeCategories, Map<String, String> executionParameters) {
                new ApLocalExecuteCommandHandler().execute(file, kwLibFile, includePlans, excludePlans, includeCategories, excludeCategories, executionParameters);
            }

            public void checkStepUrlRequired() {
                checkRequiredParam(spec, stepUrl, STEP_URL_SHORT, STEP_URL, LOCAL);
            }

            protected void handleApRemoteExecuteCommand() {
                checkStepUrlRequired();
                checkEeOptionsConsistency(spec);

                checkStepControllerVersion();

                List<ExecuteAutomationPackageTool.Report> reports = parseReportsParams();
                MavenArtifactIdentifier apMavenArtifact = getMavenArtifact(apFile);
                MavenArtifactIdentifier packageLibMavenArtifact = getMavenArtifact(packageLibrary);

                executeRemotely(stepUrl,
                        new ExecuteAutomationPackageTool.Params()
                                .setAutomationPackageFile(apMavenArtifact != null ? null : prepareApFile(apFile))
                                .setAutomationPackageMavenArtifact(apMavenArtifact)
                                .setPackageLibraryFile(packageLibMavenArtifact != null || packageLibrary == null || packageLibrary.isEmpty() ? null : prepareKPackageLibraryFile(packageLibrary))
                                .setPackageLibraryMavenArtifact(packageLibMavenArtifact)
                                .setStepProjectName(getStepProjectName())
                                .setUserId(stepUser)
                                .setAuthToken(getAuthToken())
                                .setExecutionParameters(executionParameters)
                                .setExecutionResultTimeoutS(executionTimeoutS)
                                .setWaitForExecution(!async)
                                .setEnsureExecutionSuccess(true)
                                .setIncludePlans(includePlans)
                                .setExcludePlans(excludePlans)
                                .setIncludeCategories(includeCategories)
                                .setExcludeCategories(excludeCategories)
                                .setWrapIntoTestSet(wrapIntoTestSet)
                                .setNumberOfThreads(numberOfThreads)
                                .setReports(reports)
                                .setReportOutputDir(reportDir)
                );
            }

            protected List<ExecuteAutomationPackageTool.Report> parseReportsParams() {
                List<ExecuteAutomationPackageTool.Report> reports = null;
                if (reportType != null && !reportType.isEmpty()) {
                    reports = new ArrayList<>();
                    for (String reportOption : reportType) {
                        String[] params = reportOption.split(";");
                        ExecuteAutomationPackageTool.ReportType reportTypeValue = null;
                        List<ExecuteAutomationPackageTool.ReportOutputMode> outputModes = null;
                        for (String param : params) {
                            String[] paramAndValue = param.split("=");
                            if (paramAndValue.length < 1) {
                                throw new StepCliExecutionException("Missing CLI param value: " + Arrays.toString(paramAndValue));
                            } else if (paramAndValue.length == 1) {
                                // unnamed parameter means the 'reportType'
                                reportTypeValue = ExecuteAutomationPackageTool.ReportType.valueOf(paramAndValue[0]);
                            } else if (paramAndValue[0].equalsIgnoreCase("output")) {
                                outputModes = Arrays.stream(paramAndValue[1].split(","))
                                        .map(ExecuteAutomationPackageTool.ReportOutputMode::valueOf)
                                        .collect(Collectors.toList());
                            }
                        }
                        if (reportTypeValue == null) {
                            throw new StepCliExecutionException("Unrecognized report type: " + reportOption);
                        } else if (outputModes == null) {
                            reports.add(new ExecuteAutomationPackageTool.Report(reportTypeValue));
                        } else {
                            reports.add(new ExecuteAutomationPackageTool.Report(reportTypeValue, outputModes));
                        }
                    }
                }
                return reports;
            }

            // for tests
            protected void executeRemotely(final String stepUrl,
                                           ExecuteAutomationPackageTool.Params params) {
                new ExecuteAutomationPackageTool(stepUrl, params).execute();
            }

        }

        @Override
        public Integer call() throws Exception {
            // call help by default
            return addApSubcommands(new CommandLine(new ApCommand()), ApDeployCommand::new, ApExecuteCommand::new)
                    .execute("help");
        }

    }

    public static void main(String... args) {
        int exitCode = executeMain(ApCommand.ApDeployCommand::new, ApCommand.ApExecuteCommand::new, true, args);
        System.exit(exitCode);
    }

    static int executeMain(Supplier<ApCommand.ApDeployCommand> deployCommandSupplier,
                           Supplier<ApCommand.ApExecuteCommand> executeCommandSupplier,
                           boolean lookupDefaultConfigFile,
                           String... args) {
        StepConsole configFinder = new StepConsole();

        // parse arguments just to resolve configuration files and setup default values provider programmatically
        CommandLine clForFinder = addStepSubcommands(new CommandLine(configFinder), deployCommandSupplier, executeCommandSupplier);
        CommandLine.ParseResult parseResult = null;
        try {
            parseResult = clForFinder.parseArgs(args);
        } catch (CommandLine.ParameterException ex) {
            try {
                return clForFinder.getParameterExceptionHandler().handleParseException(ex, args);
            } catch (Exception handlerException) {
                // exception during exception handling
                throw new RuntimeException("Unexpected exception", ex);
            }
        }

        List<String> customConfigFiles = null;

        // custom configuration files are only applied for "ap" command
        CommandLine.ParseResult subcommand1 = parseResult.subcommand();
        if (subcommand1 != null && Objects.equals(subcommand1.commandSpec().name(), ApCommand.AP_COMMAND)) {
            CommandLine.ParseResult subcommand2 = subcommand1.subcommand();
            if (subcommand2 != null) {
                CommandLine.Model.OptionSpec configOptionSpec = subcommand2.commandSpec().findOption(AbstractStepCommand.CONFIG);
                Object configsList = configOptionSpec == null ? null : configOptionSpec.getValue();
                if (configsList != null) {
                    customConfigFiles = ((List<String>) configsList);
                }
            }
        }

        return addStepSubcommands(new CommandLine(new StepConsole()), deployCommandSupplier, executeCommandSupplier)
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setDefaultValueProvider(new StepDefaultValuesProvider(customConfigFiles, lookupDefaultConfigFile))
                .setExecutionExceptionHandler(new StepExecutionExceptionHandler())
                .execute(args);
    }

    private static CommandLine addStepSubcommands(CommandLine cl,
                                                  Supplier<ApCommand.ApDeployCommand> deployCommandSupplier,
                                                  Supplier<ApCommand.ApExecuteCommand> executeCommandSupplier) {
        return cl.addSubcommand("help", new CommandLine.HelpCommand())
                .addSubcommand(ApCommand.AP_COMMAND,
                        addApSubcommands(new CommandLine(new ApCommand()), deployCommandSupplier, executeCommandSupplier)
                );
    }

    private static CommandLine addApSubcommands(CommandLine cl,
                                                Supplier<ApCommand.ApDeployCommand> deployCommandSupplier,
                                                Supplier<ApCommand.ApExecuteCommand> executeCommandSupplier) {
        return cl.addSubcommand("help", new CommandLine.HelpCommand())
                .addSubcommand("deploy", deployCommandSupplier.get())
                .addSubcommand("execute", executeCommandSupplier.get());
    }

}
