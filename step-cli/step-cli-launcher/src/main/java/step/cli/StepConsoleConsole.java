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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.Callable;

import static step.cli.Parameters.CONFIG;

@Command(name = "step", mixinStandardHelpOptions = true, version = "step 1.0",
        description = "The CLI interface to communicate with Step server", defaultValueProvider = StepDefaultValuesProvider.class)
public class StepConsoleConsole implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(StepConsoleConsole.class);

    @Parameters(index = "0", description = "\"Deploy\" or \"Execute\"")
    private String command;

    @Option(names = {"--" + CONFIG}, description = "The custom configuration file")
    private String config;

    @Option(names = {"--apFile"}, description = "The file with automation package")
    private File apFile;

    @Option(names = {"--stepUrl"})
    private String stepUrl;

    @Option(names = {"--projectName"})
    private String stepProjectName;

    @Option(names = {"--token"})
    private String authToken;

    @Option(names = {"--async"}, defaultValue = "true")
    private Boolean async;

    @Option(names = {"--stepUserId"})
    private String stepUserId;

    @Option(names = {"--executionTimeoutS"}, defaultValue = "3600")
    private Integer executionTimeoutS;

    @Option(names = {"-ep", "--executionParameters"})
    private Map<String, String> executionParameters;

    @Option(names = {"--waitForExecution"}, defaultValue = "true")
    private Boolean waitForExecution;

    @Option(names = {"--ensureExecutionSuccess"}, defaultValue = "true")
    private Boolean ensureExecutionSuccess;

    @Option(names = {"--includePlans"})
    private String includePlans;

    @Option(names = {"--excludePlans"})
    private String excludePlans;

    public static void main(String... args) {
        int exitCode = new CommandLine(new StepConsoleConsole()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        switch (command.toLowerCase()) {
            case "deploy":
                handleDeployCommand();
                break;
            case "execute":
                handleExecuteCommand();
                break;
            default:
                log.error("Unknown command: " + command);
                return -1;
        }
        return 0;
    }

    private void handleExecuteCommand() {
        new AbstractExecuteAutomationPackageTool(stepUrl, stepProjectName, stepUserId, authToken, executionParameters, executionTimeoutS, waitForExecution, ensureExecutionSuccess, includePlans, excludePlans) {
            @Override
            protected File getAutomationPackageFile() throws StepCliExecutionException {
                return prepareApFile(apFile);
            }
        }.execute();
    }

    protected void handleDeployCommand() {
        new AbstractDeployAutomationPackageTool(stepUrl, stepProjectName, authToken, async) {
            @Override
            protected File getFileToUpload() throws StepCliExecutionException {
                return prepareApFile(apFile);
            }
        }.execute();
    }

    protected File prepareApFile(File param) {
        try {
            if (param == null) {
                return null;
            } else if (param.isDirectory()) {
                File tempFile = Files.createTempFile(param.getName(), null).toFile();
                tempFile.deleteOnExit();
                ZipUtil.pack(param, tempFile);
                return tempFile;
            } else {
                return param;
            }
        } catch (IOException ex) {
            throw new StepCliExecutionException("Unable to prepare automation package file", ex);
        }
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setApFile(File apFile) {
        this.apFile = apFile;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public void setUrl(String url) {
        this.stepUrl = url;
    }

    public void setStepProjectName(String stepProjectName) {
        this.stepProjectName = stepProjectName;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public void setStepUrl(String stepUrl) {
        this.stepUrl = stepUrl;
    }

    public void setStepUserId(String stepUserId) {
        this.stepUserId = stepUserId;
    }

    public void setExecutionTimeoutS(Integer executionTimeoutS) {
        this.executionTimeoutS = executionTimeoutS;
    }

    public void setExecutionParameters(Map<String, String> executionParameters) {
        this.executionParameters = executionParameters;
    }

    public void setWaitForExecution(Boolean waitForExecution) {
        this.waitForExecution = waitForExecution;
    }

    public void setEnsureExecutionSuccess(Boolean ensureExecutionSuccess) {
        this.ensureExecutionSuccess = ensureExecutionSuccess;
    }

    public void setIncludePlans(String includePlans) {
        this.includePlans = includePlans;
    }

    public void setExcludePlans(String excludePlans) {
        this.excludePlans = excludePlans;
    }
}
