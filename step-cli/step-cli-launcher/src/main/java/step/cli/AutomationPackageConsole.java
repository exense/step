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
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

import static step.cli.Parameters.CONFIG;

@Command(name = "step", mixinStandardHelpOptions = true, version = "step 1.0",
        description = "The CLI interface to communicate with Step server", defaultValueProvider = StepDefaultValuesProvider.class)
public class AutomationPackageConsole implements Callable<Integer> {

    // TODO: valid config file
    public static final String DEFAULT_CONFIG_FILE = "C://temp/stepCliConfig.properties";

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageConsole.class);

    @Parameters(index = "0", description = "\"Deploy\" or \"Execute\"")
    private String command;

    @Option(names = {"--" + CONFIG}, description = "The custom configuration file", defaultValue = DEFAULT_CONFIG_FILE, showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private String config;

    @Option(names = {"--apFile"}, description = "The file with automation package")
    private File apFile;

    @Option(names = {"--stepUrl"})
    private String stepUrl;

    @Option(names = {"--artifactGroupId"})
    private String artifactGroupId;

    @Option(names = {"--artifactId"})
    private String artifactId;

    @Option(names = {"--artifactVersion"})
    private String artifactVersion;

    @Option(names = {"--artifactClassifier"})
    private String artifactClassifier;

    @Option(names = {"--projectName"})
    private String stepProjectName;

    @Option(names = {"--token"})
    private String authToken;

    @Option(names = {"--async"}, defaultValue = "true")
    private Boolean async;

    @Override
    public Integer call() throws Exception {
        switch (command.toLowerCase()) {
            case "deploy":
                handleDeployCommand();
                break;
            default:
                log.error("Unknown command: " + command);
                return -1;
        }
        return 0;
    }

    protected void handleDeployCommand() {
        new AbstractDeployAutomationPackageTool(stepUrl, artifactGroupId, artifactId, artifactVersion, artifactClassifier, stepProjectName, authToken, async) {
            @Override
            protected File getFileToUpload() throws StepCliExecutionException {
                return apFile;
            }

            @Override
            protected void logError(String errorText, Throwable e) {
                if (e != null) {
                    log.error(errorText, e);
                } else {
                    log.error(errorText);
                }
            }

            @Override
            protected void logInfo(String infoText, Throwable e) {
                if (e != null) {
                    log.info(infoText, e);
                } else {
                    log.info(infoText);
                }
            }
        }.execute();
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new AutomationPackageConsole()).execute(args);
        System.exit(exitCode);
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

    public void setArtifactGroupId(String artifactGroupId) {
        this.artifactGroupId = artifactGroupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public void setArtifactClassifier(String artifactClassifier) {
        this.artifactClassifier = artifactClassifier;
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

}
