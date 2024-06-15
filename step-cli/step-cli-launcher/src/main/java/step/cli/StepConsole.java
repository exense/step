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
import picocli.CommandLine.Parameters;
import step.automation.packages.AutomationPackageFromFolderProvider;
import step.automation.packages.AutomationPackageReadingException;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static step.cli.Parameters.CONFIG;

@Command(name = "step", mixinStandardHelpOptions = true, version = "step 1.0",
        description = "The CLI interface to communicate with Step server", defaultValueProvider = StepDefaultValuesProvider.class)
public class StepConsole implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(StepConsole.class);

    @Parameters(index = "0", description = "The context of command. Supported values: \"ap\"")
    private String context;

    @Parameters(index = "1", description = "For \"ap\": \"deploy\", \"execute\"")
    private String command;

    @Option(names = {"-" + CONFIG}, description = "The custom configuration file(s)")
    private List<String> config;

    @Option(names = {"-p", "--package"}, description = "The file or folder with automation package")
    private File apFile;

    @Option(names = {"-u", "--stepUrl"}, description = "The URL of Step server")
    private String stepUrl;

    @CommandLine.ArgGroup(validate = false, heading = "The security parameters (for Step EE only)%n")
    protected SecurityParams securityParams = new SecurityParams();

    @CommandLine.ArgGroup(validate = false, heading = "Special parameters for \"ap deploy\"%n")
    protected ApDeployParams apDeployParams = new ApDeployParams();

    @CommandLine.ArgGroup(validate = false, heading = "The parameters for \"ap execute\"%n")
    protected ApExecuteParams apExecuteParams = new ApExecuteParams();

    protected static class SecurityParams {
        @Option(names = {"--projectName"})
        private String stepProjectName;

        @Option(names = {"--token"})
        private String authToken;

        @Option(names = {"--stepUserId"})
        private String stepUserId;
    }

    protected static class ApDeployParams {
        @Option(names = {"--async"}, defaultValue = "true")
        protected Boolean async;
    }

    protected static class ApExecuteParams {
        @Option(names = {"--executionTimeoutS"}, defaultValue = "3600")
        protected Integer executionTimeoutS;

        @Option(names = {"--waitForExecution"}, defaultValue = "true")
        protected Boolean waitForExecution;

        @Option(names = {"--ensureExecutionSuccess"}, defaultValue = "true")
        protected Boolean ensureExecutionSuccess;

        @Option(names = {"--includePlans"}, description = "The comma separated list of plans to be executed in \"ap execute\" command")
        protected String includePlans;

        @Option(names = {"--excludePlans"}, description = "The comma separated list of plans to be excluded from execution in \"ap execute\" command")
        protected String excludePlans;

        @Option(names = {"--local"}, defaultValue = "false", description = "Flag to run AP locally for \"ap execute\" command")
        private boolean local;

        @Option(names = {"-ep", "--executionParameters"}, description = "The execution parameters to be used in \"ap execute\" command")
        protected Map<String, String> executionParameters;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new StepConsole()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if ("ap".equalsIgnoreCase(context)) {
            switch (command.toLowerCase()) {
                case "deploy":
                    handleApDeployCommand();
                    break;
                case "execute":
                    if (!apExecuteParams.local) {
                        handleApRemoteExecuteCommand();
                    } else {
                        handleApLocalExecuteCommand();
                    }
                    break;
                default:
                    log.error("Unknown command: " + command);
                    return -1;
            }
        } else {
            log.error("Unknown context: " + context);
            return -1;
        }
        return 0;
    }

    private void handleApLocalExecuteCommand() {
        File file = prepareApFile(apFile);
        if (file == null) {
            throw new StepCliExecutionException("AP file is not defined");
        }
        new ApLocalExecuteCommandHandler().execute(file, apExecuteParams.includePlans, apExecuteParams.excludePlans, apExecuteParams.executionParameters);
    }

    protected void handleApRemoteExecuteCommand() {
        new AbstractExecuteAutomationPackageTool(
                stepUrl, securityParams.stepProjectName, securityParams.stepUserId, securityParams.authToken,
                apExecuteParams.executionParameters, apExecuteParams.executionTimeoutS,
                apExecuteParams.waitForExecution, apExecuteParams.ensureExecutionSuccess,
                apExecuteParams.includePlans, apExecuteParams.excludePlans
        ) {
            @Override
            protected File getAutomationPackageFile() throws StepCliExecutionException {
                return prepareApFile(apFile);
            }
        }.execute();
    }

    protected void handleApDeployCommand() {
        new AbstractDeployAutomationPackageTool(stepUrl, securityParams.stepProjectName, securityParams.authToken, apDeployParams.async) {
            @Override
            protected File getFileToUpload() throws StepCliExecutionException {
                return prepareApFile(apFile);
            }
        }.execute();
    }

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

    public void setCommand(String command) {
        this.command = command;
    }

    public void setApFile(File apFile) {
        this.apFile = apFile;
    }

    public void setConfig(List<String> config) {
        this.config = config;
    }

    public void setUrl(String url) {
        this.stepUrl = url;
    }

    public void setStepProjectName(String stepProjectName) {
        this.securityParams.stepProjectName = stepProjectName;
    }

    public void setAuthToken(String authToken) {
        this.securityParams.authToken = authToken;
    }

    public void setAsync(Boolean async) {
        this.apDeployParams.async = async;
    }

    public void setStepUrl(String stepUrl) {
        this.stepUrl = stepUrl;
    }

    public void setStepUserId(String stepUserId) {
        this.securityParams.stepUserId = stepUserId;
    }

    public void setExecutionTimeoutS(Integer executionTimeoutS) {
        this.apExecuteParams.executionTimeoutS = executionTimeoutS;
    }

    public void setExecutionParameters(Map<String, String> executionParameters) {
        this.apExecuteParams.executionParameters = executionParameters;
    }

    public void setWaitForExecution(Boolean waitForExecution) {
        this.apExecuteParams.waitForExecution = waitForExecution;
    }

    public void setEnsureExecutionSuccess(Boolean ensureExecutionSuccess) {
        this.apExecuteParams.ensureExecutionSuccess = ensureExecutionSuccess;
    }

    public void setIncludePlans(String includePlans) {
        this.apExecuteParams.includePlans = includePlans;
    }

    public void setExcludePlans(String excludePlans) {
        this.apExecuteParams.excludePlans = excludePlans;
    }

    public void setLocal(boolean local) {
        this.apExecuteParams.local = local;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
