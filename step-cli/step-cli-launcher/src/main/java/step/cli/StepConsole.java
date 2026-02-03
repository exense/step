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
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import step.client.controller.ControllerServicesClient;
import step.client.credentials.ControllerCredentials;
import step.core.Constants;
import step.core.Version;
import step.core.maven.MavenArtifactIdentifier;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;


@Command(name = "step",
        mixinStandardHelpOptions = true,
        version = Constants.STEP_VERSION_STRING,
        description = "The command-line interface (CLI) to interact with Step",
        usageHelpAutoWidth = true
)
public class StepConsole implements Callable<Integer> {

    public static final String REQUIRED_ERR_MESSAGE = "Illegal parameters. One of the following options is required: '%s'";

    public static final String AP_IGNORE_NAME = ".apignore";

    public static final Logger log = LoggerFactory.getLogger(StepConsole.class);
    public static final String MANAGED = "managed:";

    @Override
    public Integer call() throws Exception {
        // call help by default
        return addStepSubcommands(new CommandLine(new StepConsole()), ApCommand.ApDeployCommand::new, ApCommand.ApExecuteCommand::new,
                            LibraryCommand.DeployLibraryCommand::new)
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

        public void checkStepUrlRequired() {
            checkRequiredParam(spec, stepUrl, STEP_URL_SHORT, STEP_URL);
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
            if (MavenArtifactIdentifier.isMvnIdentifierShortString(apFile)) {
                return MavenArtifactIdentifier.fromShortString(apFile);
            } else {
                return null;
            }
        }

        protected String getManagedLibraryName(String managedLibraryName) {
            if (managedLibraryName != null && managedLibraryName.startsWith(MANAGED)) {
                return managedLibraryName.substring(MANAGED.length());
            } else {
                return null;
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
                    StepConsole.log.warn(warn);
                } else {
                    String err = "Version mismatch. The server version (" + e.getResult().getServerVersion() + ") is incompatible with the current CLI version (" + e.getResult().getClientVersion() + "). Please ensure both the CLI and server are running compatible versions.";
                    if (!force) {
                        err += " You can use the " + FORCE + " option to ignore this validation.";
                        throw new StepCliExecutionException(err, e);
                    } else {
                        StepConsole.log.warn(err);
                    }
                }
            }
        }

        protected void checkAll() {
            checkStepUrlRequired();
            checkEeOptionsConsistency(spec);
            checkStepControllerVersion();
        }

        protected Version getVersion() {
            return Constants.STEP_VERSION;
        }

        @Override
        public Integer call() throws Exception {
            printConfigIfRequired();
            return 0;
        }
    }

    public static void main(String... args) {
        int exitCode = executeMain(ApCommand.ApDeployCommand::new, ApCommand.ApExecuteCommand::new, LibraryCommand.DeployLibraryCommand::new, true, args);
        System.exit(exitCode);
    }

    static int executeMain(Supplier<ApCommand.ApDeployCommand> deployCommandSupplier,
                           Supplier<ApCommand.ApExecuteCommand> executeCommandSupplier,
                           Supplier<LibraryCommand.DeployLibraryCommand> deployLibraryCommandSupplier,
                           boolean lookupDefaultConfigFile,
                           String... args) {
        StepConsole configFinder = new StepConsole();

        // parse arguments just to resolve configuration files and setup default values provider programmatically
        CommandLine clForFinder = addStepSubcommands(new CommandLine(configFinder), deployCommandSupplier, executeCommandSupplier, deployLibraryCommandSupplier);
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
        if (subcommand1 != null && (Objects.equals(subcommand1.commandSpec().name(), ApCommand.AP_COMMAND) ||
                Objects.equals(subcommand1.commandSpec().name(), LibraryCommand.LIBRARY_COMMAND) )) {
            CommandLine.ParseResult subcommand2 = subcommand1.subcommand();
            if (subcommand2 != null) {
                CommandLine.Model.OptionSpec configOptionSpec = subcommand2.commandSpec().findOption(AbstractStepCommand.CONFIG);
                Object configsList = configOptionSpec == null ? null : configOptionSpec.getValue();
                if (configsList != null) {
                    customConfigFiles = ((List<String>) configsList);
                }
            }
        }

        return addStepSubcommands(new CommandLine(new StepConsole()), deployCommandSupplier, executeCommandSupplier, deployLibraryCommandSupplier)
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setDefaultValueProvider(new StepDefaultValuesProvider(customConfigFiles, lookupDefaultConfigFile))
                .setExecutionExceptionHandler(new StepExecutionExceptionHandler())
                .execute(args);
    }

    private static CommandLine addStepSubcommands(CommandLine cl,
                                                  Supplier<ApCommand.ApDeployCommand> deployCommandSupplier,
                                                  Supplier<ApCommand.ApExecuteCommand> executeCommandSupplier,
                                                  Supplier<LibraryCommand.DeployLibraryCommand> deployLibraryCommandSupplier) {
        return cl.addSubcommand("help", new CommandLine.HelpCommand())
                .addSubcommand(ApCommand.AP_COMMAND,
                        addApSubcommands(new CommandLine(new ApCommand()), deployCommandSupplier, executeCommandSupplier))
                .addSubcommand(LibraryCommand.LIBRARY_COMMAND, addLibrarySubcommands(new CommandLine(new LibraryCommand()), deployLibraryCommandSupplier));
    }

    public static CommandLine addApSubcommands(CommandLine cl,
                                               Supplier<ApCommand.ApDeployCommand> deployCommandSupplier,
                                               Supplier<ApCommand.ApExecuteCommand> executeCommandSupplier) {
        return cl.addSubcommand("help", new CommandLine.HelpCommand())
                .addSubcommand("deploy", deployCommandSupplier.get())
                .addSubcommand("execute", executeCommandSupplier.get());
    }

    public static CommandLine addLibrarySubcommands(CommandLine cl,
                                               Supplier<LibraryCommand.DeployLibraryCommand> deployCommandSupplier) {
        return cl.addSubcommand("help", new CommandLine.HelpCommand())
                .addSubcommand("deploy", deployCommandSupplier.get());
    }

}
