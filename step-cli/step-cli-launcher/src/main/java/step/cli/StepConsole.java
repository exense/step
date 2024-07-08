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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static step.cli.Parameters.CONFIG;


@Command(name = "step",
        mixinStandardHelpOptions = true,
        version = "step 1.0",
        description = "The CLI interface to communicate with Step server",
        subcommands = {
                StepConsole.ApCommand.class,
                CommandLine.HelpCommand.class
        })
public class StepConsole implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(StepConsole.class);

    @Override
    public Integer call() throws Exception {
        // call help by default
        return new CommandLine(new StepConsole()).execute("help");
    }

    public static abstract class AbstractStepCommand {

        @Option(names = {"-" + CONFIG}, description = "The custom configuration file(s)")
        protected List<String> config;

        @Option(names = {"-u", "--stepUrl"}, description = "The URL of Step server")
        protected String stepUrl;

        @Option(names = {"--projectName"}, description = "The project name in Step")
        protected String stepProjectName;

        @Option(names = {"--token"})
        protected String authToken;

        @Option(names = {"--stepUserId"})
        protected String stepUserId;
    }

    @Command(name = "ap",
            mixinStandardHelpOptions = true,
            version = "step.ap 1.0",
            description = "The CLI interface to manage automation packages in Step",
            defaultValueProvider = StepDefaultValuesProvider.class,
            subcommands = {
                    ApCommand.ApDeployCommand.class,
                    ApCommand.ApExecuteCommand.class,
                    CommandLine.HelpCommand.class
            })
    public static class ApCommand implements Callable<Integer> {

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
                defaultValueProvider = StepDefaultValuesProvider.class,
                subcommands = {CommandLine.HelpCommand.class})
        public static class ApDeployCommand extends AbstractApCommand implements Callable<Integer> {

            @Option(names = {"--async"}, defaultValue = "true", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
            protected Boolean async;

            @Override
            public Integer call() throws Exception {
                handleApDeployCommand();
                return 0;
            }

            protected void handleApDeployCommand() {
                new AbstractDeployAutomationPackageTool(stepUrl, stepProjectName, authToken, async) {
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
                defaultValueProvider = StepDefaultValuesProvider.class,
                subcommands = {CommandLine.HelpCommand.class})
        public static class ApExecuteCommand extends AbstractApCommand implements Callable<Integer> {

            @Option(names = {"--executionTimeoutS"}, defaultValue = "3600")
            protected Integer executionTimeoutS;

            @Option(names = {"--waitForExecution"}, defaultValue = "true", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
            protected Boolean waitForExecution;

            @Option(names = {"--ensureExecutionSuccess"}, defaultValue = "true", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
            protected Boolean ensureExecutionSuccess;

            @Option(names = {"--includePlans"}, description = "The comma separated list of plans to be executed")
            protected String includePlans;

            @Option(names = {"--excludePlans"}, description = "The comma separated list of plans to be excluded from execution")
            protected String excludePlans;

            @Option(names = {"--local"}, defaultValue = "false", description = "The flag to run AP locally ", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
            protected boolean local;

            @Option(names = {"-ep", "--executionParameters"}, description = "The execution parameters to be used ")
            protected Map<String, String> executionParameters;

            @Override
            public Integer call() throws Exception {
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
                new AbstractExecuteAutomationPackageTool(
                        stepUrl, stepProjectName, stepUserId, authToken,
                        executionParameters, executionTimeoutS,
                        waitForExecution, ensureExecutionSuccess,
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
            return new CommandLine(new ApCommand()).execute("help");
        }

    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new StepConsole()).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
        System.exit(exitCode);
    }

}
