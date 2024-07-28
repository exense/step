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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import step.automation.packages.AutomationPackageFromFolderProvider;
import step.automation.packages.AutomationPackageReadingException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;


@Command(name = "step",
        mixinStandardHelpOptions = true,
        version = "step 1.0",
        description = "The CLI interface to communicate with Step server",
        subcommands = {
                StepConsole.ApCommand.class,
                CommandLine.HelpCommand.class
        })
public class StepConsole implements Callable<Integer> {

    public static final String REQUIRED_ERR_MESSAGE = "Illegal parameters. One of the following options is required: '%s'";

    private static final Logger log = LoggerFactory.getLogger(StepConsole.class);

    @Override
    public Integer call() throws Exception {
        // call help by default
        return new CommandLine(new StepConsole())
                .setExecutionExceptionHandler(new StepExecutionExceptionHandler())
                .execute("help");
    }

    public static abstract class AbstractStepCommand implements Callable<Integer> {

        public static final String STEP_URL_SHORT = "-u";
        public static final String STEP_URL = "--stepUrl";
        public static final String PROJECT_NAME = "--projectName";
        public static final String TOKEN = "--token";
        public static final String VERBOSE = "--verbose";
        public static final String CONFIG = "-c";

        @CommandLine.Spec
        protected CommandLine.Model.CommandSpec spec;

        @Option(names = {CONFIG}, description = "The custom configuration file(s)")
        protected List<String> config;

        @Option(names = {STEP_URL_SHORT, STEP_URL}, description = "The URL of Step server")
        protected String stepUrl;

        @Option(names = {PROJECT_NAME}, description = "The project name in Step")
        protected String stepProjectName;

        @Option(names = {TOKEN})
        protected String authToken;

        @Option(names = {"--stepUserId"})
        protected String stepUserId;

        @Option(names = {VERBOSE}, defaultValue = "false")
        protected boolean verbose;

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
            if(getAuthToken() != null && !getAuthToken().isEmpty()){
                checkRequiredParam(spec, getStepProjectName(), PROJECT_NAME);
            }

            if(getStepProjectName() != null && !getStepProjectName().isEmpty()){
                checkRequiredParam(spec, getAuthToken(), TOKEN);
            }
        }

        public void checkStepUrlRequired() {
            checkRequiredParam(spec, stepUrl, STEP_URL_SHORT, STEP_URL);
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

        @Override
        public Integer call() throws Exception {
            printConfigIfRequired();
            return 0;
        }
    }

    @Command(name = ApCommand.AP_COMMAND,
            mixinStandardHelpOptions = true,
            version = "step.ap 1.0",
            description = "The CLI interface to manage automation packages in Step",
            subcommands = {
                    ApCommand.ApDeployCommand.class,
                    ApCommand.ApExecuteCommand.class,
                    CommandLine.HelpCommand.class
            })
    public static class ApCommand implements Callable<Integer> {

        public static final String AP_COMMAND = "ap";

        public static abstract class AbstractApCommand extends AbstractStepCommand {

            @Option(names = {"-p", "--package"}, description = "The file or folder with automation package")
            protected File apFile;

            /**
             * If the param points to the folder, prepares the zipped AP file with .stz extension.
             * Otherwise, if the param is a simple file, just returns this file
             *
             * @param param the source of AP
             */
            protected File prepareApFile(File param) {
                try {
                    if (param == null) {
                        // use the current folder by default
                        param = new File(new File("").getAbsolutePath());
                    }
                    log.info("The automation package source is {}", param.getAbsolutePath());

                    if (param.isDirectory()) {
                        // check if the folder is AP (contains the yaml descriptor)
                        checkApFolder(param);

                        File tempDirectory = Files.createTempDirectory("stepcli").toFile();
                        tempDirectory.deleteOnExit();
                        File tempFile = new File(tempDirectory, param.getName() + ".stz");
                        tempFile.deleteOnExit();
                        log.info("Preparing AP archive: {}", tempFile.getAbsolutePath());
                        ZipUtil.pack(param, tempFile);
                        return tempFile;
                    } else {
                        return param;
                    }
                } catch (IOException ex) {
                    throw new StepCliExecutionException("Unable to prepare automation package file", ex);
                }
            }

            private void checkApFolder(File param) throws IOException {
                try (AutomationPackageFromFolderProvider apProvider = new AutomationPackageFromFolderProvider(param)) {
                    try {
                        if (!apProvider.getAutomationPackageArchive().hasAutomationPackageDescriptor()) {
                            throw new StepCliExecutionException("The AP folder " + param.getAbsolutePath() + " doesn't contain the AP descriptor file");
                        }
                    } catch (AutomationPackageReadingException e) {
                        throw new StepCliExecutionException("Unable to read automation package from folder " + param.getAbsolutePath(), e);
                    }
                }
            }

        }

        @Command(name = "deploy",
                mixinStandardHelpOptions = true,
                version = "step.ap.deploy 1.0",
                description = "The CLI interface to deploy automation packages in Step",
                subcommands = {CommandLine.HelpCommand.class})
        public static class ApDeployCommand extends AbstractApCommand {

            @Option(names = {"--async"}, defaultValue = "false", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
            protected boolean async;

            @Override
            public Integer call() throws Exception {
                super.call();
                handleApDeployCommand();
                return 0;
            }

            protected void handleApDeployCommand() {
                checkStepUrlRequired();
                checkEeOptionsConsistency(spec);
                new AbstractDeployAutomationPackageTool(stepUrl, getStepProjectName(), getAuthToken(), async) {
                    @Override
                    protected File getFileToUpload() throws StepCliExecutionException {
                        return prepareApFile(apFile);
                    }
                }.execute();
            }
        }

        @Command(name = "execute",
                mixinStandardHelpOptions = true,
                version = "step.ap.execute 1.0",
                description = "The CLI interface to execute automation packages in Step",
                subcommands = {CommandLine.HelpCommand.class})
        public static class ApExecuteCommand extends AbstractApCommand implements Callable<Integer> {

            public static final String EP_DESCRIPTION_KEY = "executionParameters";

            @Option(names = {"--executionTimeoutS"}, defaultValue = "3600")
            protected Integer executionTimeoutS;

            @Option(names = {"--async"}, defaultValue = "false", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
            protected boolean async;

            @Option(names = {"--ensureExecutionSuccess"}, defaultValue = "true", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
            protected boolean ensureExecutionSuccess;

            @Option(names = {"--includePlans"}, description = "The comma separated list of plans to be executed")
            protected String includePlans;

            @Option(names = {"--excludePlans"}, description = "The comma separated list of plans to be excluded from execution")
            protected String excludePlans;

            @Option(names = {"--local"}, defaultValue = "false", description = "The flag to run AP locally ", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
            protected boolean local;

            @Option(descriptionKey = EP_DESCRIPTION_KEY, names = {"-ep", "--executionParameters"}, description = "The execution parameters to be used ", split = "\\|", splitSynopsisLabel = "|")
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
                    new CommandLine(defaultExecutionParametersLookup).setDefaultValueProvider(customDefaultValuesProvider).parseArgs();

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
                new ApLocalExecuteCommandHandler().execute(file, includePlans, excludePlans, executionParameters);
            }

            protected void handleApRemoteExecuteCommand() {
                checkStepUrlRequired();
                checkEeOptionsConsistency(spec);
                new AbstractExecuteAutomationPackageTool(
                        stepUrl, getStepProjectName(), stepUserId, getAuthToken(),
                        executionParameters, executionTimeoutS,
                        !async, ensureExecutionSuccess,
                        includePlans, excludePlans
                ) {
                    @Override
                    protected File getAutomationPackageFile() throws StepCliExecutionException {
                        return prepareApFile(apFile);
                    }
                }.execute();
            }

        }

        @Override
        public Integer call() throws Exception {
            // call help by default
            return new CommandLine(new ApCommand())
                    .setExecutionExceptionHandler(new StepExecutionExceptionHandler())
                    .execute("help");
        }

    }

    public static void main(String... args) {
        StepConsole configFinder = new StepConsole();

        // parse arguments just to resolve configuration files and setup default values provider programmatically
        CommandLine.ParseResult parseResult = new CommandLine(configFinder).parseArgs(args);
        List<String> customConfigFiles = null;

        // custom configuration files are only applied for "ap" command
        if (Objects.equals(parseResult.subcommand().commandSpec().name(), ApCommand.AP_COMMAND)) {
            Object configsList = parseResult.subcommand().subcommand().commandSpec().findOption(AbstractStepCommand.CONFIG).getValue();
            if (configsList != null) {
                customConfigFiles = ((List<String>) configsList);
            }
        }

        int exitCode = new CommandLine(configFinder)
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setDefaultValueProvider(new StepDefaultValuesProvider(customConfigFiles))
                .setExecutionExceptionHandler(new StepExecutionExceptionHandler())
                .execute(args);
        System.exit(exitCode);
    }

}
