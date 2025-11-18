package step.cli;

import ch.exense.commons.io.FileHelper;
import picocli.CommandLine;
import step.automation.packages.AutomationPackageArchive;
import step.automation.packages.AutomationPackageFromFolderProvider;
import step.automation.packages.AutomationPackageReadingException;
import step.cli.apignore.ApIgnoreFileFilter;
import step.cli.parameters.ApDeployParameters;
import step.cli.parameters.ApExecuteParameters;
import step.core.Constants;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(name = ApCommand.AP_COMMAND,
        mixinStandardHelpOptions = true,
        version = Constants.STEP_API_VERSION_STRING,
        description = "The CLI interface to manage automation packages in Step",
        usageHelpAutoWidth = true
)
public class ApCommand implements Callable<Integer> {

    public static final String AP_COMMAND = "ap";

    public static abstract class AbstractApCommand extends StepConsole.AbstractStepCommand {

        @CommandLine.Option(names = {"-p", "--package"}, paramLabel = "<Package>", description = "The path to the automation-package.yaml file, folder or the archive containing it. Also supports maven coordinates (mvn:groupId:artefactId:version[:classifier:type]).")
        protected String apFile;

        @CommandLine.Option(names = {"-l", "--library"}, paramLabel = "<Library>", description = "The file path, maven coordinate (mvn:groupId:artefactId:version[:classifier:type]), or the name of an existing managed library (example managed:MY_COMMON_LIBRARY).")
        protected String library;

        /**
         * If the param points to the folder, prepares the zipped AP file with .stz extension.
         * Otherwise, if the param is a simple file, just returns this file
         *
         * @param param the source of AP
         */
        protected static File prepareFile(String param, String entityNameForLog, boolean checkApFolder) {
            try {
                File file = null;
                if (param == null) {
                    // use the current folder by default
                    file = new File(new File("").getAbsolutePath());
                } else {
                    file = new File(param);
                }
                StepConsole.log.info("The {} is {}", entityNameForLog, file.getAbsolutePath());

                if (file.isDirectory()) {
                    // check if the folder is AP (contains the yaml descriptor)
                    if (checkApFolder) {
                        checkApFolder(file);
                    }

                    Function<File, Boolean> fileFilter = null;
                    File apIgnoreFile = new File(file, StepConsole.AP_IGNORE_NAME);
                    if (apIgnoreFile.exists()) {
                        ApIgnoreFileFilter gitIgnore = new ApIgnoreFileFilter(file.toPath(), apIgnoreFile.toPath());
                        fileFilter = file1 -> !file1.getName().equals(StepConsole.AP_IGNORE_NAME) && gitIgnore.accept(file1.toPath());
                    }

                    File tempDirectory = Files.createTempDirectory("stepcli").toFile();
                    tempDirectory.deleteOnExit();
                    File tempFile = new File(tempDirectory, file.getName() + ".stz");
                    tempFile.deleteOnExit();
                    StepConsole.log.info("Preparing AP archive: {}", tempFile.getAbsolutePath());
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

        protected static File preparePackageLibraryFile(String param) {
            return prepareFile(param, "package library", false);
        }

        private static void checkApFolder(File param) throws IOException {
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
    }

    @CommandLine.Command(name = "deploy",
            mixinStandardHelpOptions = true,
            version = Constants.STEP_API_VERSION_STRING,
            description = "The CLI interface to deploy automation packages in Step",
            usageHelpAutoWidth = true,
            subcommands = {CommandLine.HelpCommand.class})
    public static class ApDeployCommand extends AbstractApCommand {

        public static final String VERSION_NAME = "--versionName";
        public static final String FORCE_REFRESH_OF_SNAPSHOTS = "--forceRefreshOfSnapshots";

        @CommandLine.Option(names = {"--async"}, defaultValue = "false", showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
                description = "Whether to waits for the deployment to complete")
        protected boolean async;

        @CommandLine.Option(names = {VERSION_NAME}, description = "Optionally set the version of this automation package. This allows to deploy and use multiple versions of the same package on Step. If a version is set, them the activation expression is required too.")
        protected String versionName;

        @CommandLine.Option(names = {"--activationExpression"}, description = "When deploying multiple versions of the same package (see \"apVersion\"), the expression is used to select the proper versions during the execution of plans. Example: \"env == PROD\"")
        protected String activationExpression;

        @CommandLine.Option(names = {FORCE_REFRESH_OF_SNAPSHOTS}, defaultValue = "false",
                description = "To force the refresh of snapshot content when available in the remote repository, this will trigger reloading all automation packages using the same snapshot artefact in case of update.")
        protected boolean forceRefreshOfSnapshots;

        @Override
        public Integer call() throws Exception {
            super.call();
            handleApDeployCommand();
            return 0;
        }

        protected void handleApDeployCommand() {
            checkAll();
            MavenArtifactIdentifier apMavenArtifact = getMavenArtifact(apFile);
            MavenArtifactIdentifier packageLibraryMavenArtifact = getMavenArtifact(library);
            String managedLibraryName = getManagedLibraryName(library);

            ApDeployParameters params = new ApDeployParameters()
                    .setAsync(async)
                    .setForceRefreshOfSnapshots(forceRefreshOfSnapshots)
                    .setAutomationPackageMavenArtifact(apMavenArtifact)
                    .setAutomationPackageFile(apMavenArtifact != null ? null : prepareApFile(apFile))
                    .setStepProjectName(getStepProjectName())
                    .setAuthToken(getAuthToken())
                    .setVersionName(versionName)
                    .setActivationExpression(activationExpression)
                    .setlibraryMavenArtifact(packageLibraryMavenArtifact)
                    .setManagedLibraryName(managedLibraryName)
                    .setLibraryFile(packageLibraryMavenArtifact != null || managedLibraryName != null || library == null || library.isEmpty() ? null : preparePackageLibraryFile(library));
            executeTool(stepUrl, params);
        }

        // for tests
        protected void executeTool(final String stepUrl, ApDeployParameters params) {
            new DeployAutomationPackageTool(stepUrl, params).execute();
        }
    }

    @CommandLine.Command(name = "execute",
            mixinStandardHelpOptions = true,
            version = "step.ap.execute 1.0",
            description = "The CLI interface to execute automation packages in Step",
            usageHelpAutoWidth = true,
            subcommands = {CommandLine.HelpCommand.class})
    public static class ApExecuteCommand extends AbstractApCommand implements Callable<Integer> {

        public static final String EP_DESCRIPTION_KEY = "executionParameters";

        @CommandLine.Option(names = {"--executionTimeoutS"}, defaultValue = "3600", description = "Maximum time in seconds to wait for executions completeness")
        protected Integer executionTimeoutS;

        @CommandLine.Option(names = {"--async"}, defaultValue = "false", showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
                description = "Whether to wait for execution completeness")
        protected boolean async;

        @CommandLine.Option(names = {"--includePlans"}, description = "The comma separated list of plans to be executed")
        protected String includePlans;

        @CommandLine.Option(names = {"--excludePlans"}, description = "The comma separated list of plans to be excluded from execution")
        protected String excludePlans;

        @CommandLine.Option(names = {"--includeCategories"}, description = "The comma separated list of categories to be executed")
        protected String includeCategories;

        @CommandLine.Option(names = {"--excludeCategories"}, description = "The comma separated list of categories to be excluded from execution")
        protected String excludeCategories;

        @CommandLine.Option(names = {LOCAL}, defaultValue = "false", description = "To execute the Automation Package locally ", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
        protected boolean local;

        @CommandLine.Option(names = {"--wrapIntoTestSet"}, defaultValue = "false", description = "To wrap all executed plans into the single test set", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
        protected boolean wrapIntoTestSet;

        @CommandLine.Option(names = {"--numberOfThreads"}, description = "Max number of threads to be used for execution in case of wrapped test set")
        protected Integer numberOfThreads;

        @CommandLine.Option(names = {"--reportType"}, description = "The type of execution report to be generated and stored locally. Supported report types: junit, aggregated. Also (optional) you can specify the output destination: --reportType=junit;output=file,stdout")
        protected List<String> reportType;

        @CommandLine.Option(names = {"--reportDir"}, description = "The local folder to store generated execution reports", defaultValue = "reports")
        protected File reportDir;

        @CommandLine.Option(descriptionKey = EP_DESCRIPTION_KEY, names = {"-ep", "--executionParameters"}, description = "Set execution parameters for local and remote executions ", split = "\\|", splitSynopsisLabel = "|")
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

            StepConsole.log.info("Execute automation package with parameters: {}", executionParameters);
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

            File packageLibraryFile = null;
            if (library != null && !library.isEmpty()) {
                // TODO: SED-4035 - classloader issue for local execution with library - should be fixed later
                throw new StepCliExecutionException("Libraries are not supported for local execution");
//                packageLibraryFile = preparePackageLibraryFile(library);
            }

            executeLocally(file, packageLibraryFile, includePlans, excludePlans, includeCategories, excludeCategories, executionParameters);
        }

        protected void executeLocally(File file, File libFile, String includePlans, String excludePlans, String includeCategories,
                                      String excludeCategories, Map<String, String> executionParameters) {
            new ApLocalExecuteCommandHandler().execute(file, libFile, includePlans, excludePlans, includeCategories, excludeCategories, executionParameters);
        }

        public void checkStepUrlRequired() {
            checkRequiredParam(spec, stepUrl, STEP_URL_SHORT, STEP_URL, LOCAL);
        }

        protected void handleApRemoteExecuteCommand() {
            checkAll();

            List<ExecuteAutomationPackageTool.Report> reports = parseReportsParams();
            MavenArtifactIdentifier apMavenArtifact = getMavenArtifact(apFile);
            MavenArtifactIdentifier packageLibMavenArtifact = getMavenArtifact(library);
            String managedLibraryName = getManagedLibraryName(library);

            executeRemotely(stepUrl,
                    new ApExecuteParameters()
                            .setAutomationPackageFile(apMavenArtifact != null ? null : prepareApFile(apFile))
                            .setAutomationPackageMavenArtifact(apMavenArtifact)
                            .setLibraryFile(packageLibMavenArtifact != null || managedLibraryName != null || library == null || library.isEmpty() ? null : preparePackageLibraryFile(library))
                            .setlibraryMavenArtifact(packageLibMavenArtifact)
                            .setManagedLibraryName(managedLibraryName)
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
                                       ApExecuteParameters params) {
            new ExecuteAutomationPackageTool(stepUrl, params).execute();
        }

    }

    @Override
    public Integer call() throws Exception {
        // call help by default
        return StepConsole.addApSubcommands(new CommandLine(new ApCommand()), ApDeployCommand::new, ApExecuteCommand::new)
                .execute("help");
    }

}
