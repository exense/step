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
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import static step.cli.Parameters.*;
import static step.cli.Parameters.ARTIFACT_ID;

@Command(name = "step", mixinStandardHelpOptions = true, version = "step 1.0",
        description = "The CLI interface to communicate with Step server")
public class AutomationPackageConsole implements Callable<Integer> {

    public static final String DEFAULT_CONFIG_FILE = "C://temp/stepCliConfig.properties";

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageConsole.class);

    @Parameters(index = "0", description = "\"Deploy\" or \"Execute\"")
    private String command;

    // TODO: valid default path to config file
    @Option(names = {PREFIX + CONFIG}, description = "The custom configuration file", defaultValue = DEFAULT_CONFIG_FILE)
    private String config;

    @Option(names = {PREFIX + AP_FILE}, description = "The file with automation package")
    private String apFile;

    @Option(names = {PREFIX + STEP_URL})
    private String stepUrl;

    @Option(names = {PREFIX + ARTIFACT_GROUP_ID})
    private String artifactGroupId;

    @Option(names = {PREFIX + ARTIFACT_ID})
    private String artifactId;

    @Option(names = {PREFIX + ARTIFACT_VERSION})
    private String artifactVersion;

    @Option(names = {PREFIX + ARTIFACT_CLASSIFIER})
    private String artifactClassifier;

    @Option(names = {PREFIX + PROJECT_NAME})
    private String stepProjectName;

    @Option(names = {PREFIX + TOKEN})
    private String authToken;

    @Option(names = {PREFIX + ASYNC}, defaultValue = "true")
    private String async;

    private CliCommandHandler apDeployHandler = new ApDeployCliHandler();

    @Override
    public Integer call() throws Exception {
        CliConfig config = readConfig();
        switch (command.toLowerCase()) {
            case "deploy":
                apDeployHandler.execute(config);
                break;
            default:
                System.out.println("Unknown command: " + command);
                return -1;
        }
        return 0;
    }

    private CliConfig readConfig() {
        CliConfig res = readConfigFile(config);
        // TODO: run over all fields annotated with @Option via reflection
        addConsoleParam(STEP_URL, stepUrl, res);
        addConsoleParam(AP_FILE, apFile, res);
        addConsoleParam(STEP_URL, stepUrl, res);
        addConsoleParam(ARTIFACT_GROUP_ID, artifactGroupId, res);
        addConsoleParam(ARTIFACT_ID, artifactId, res);
        addConsoleParam(ARTIFACT_VERSION, artifactVersion, res);
        addConsoleParam(ARTIFACT_CLASSIFIER, artifactClassifier, res);
        addConsoleParam(PROJECT_NAME, stepProjectName, res);
        addConsoleParam(TOKEN, authToken, res);
        addConsoleParam(ASYNC, async, res);
        return res;
    }

    private void addConsoleParam(String name, String value, CliConfig res) {
        if (value != null) {
            res.getConfig().put(name, value);
        }
    }

    public CliConfig readConfigFile(String configFilePath) throws StepCliExecutionException {
        try {
            CliConfig res = new CliConfig();
            File configFile = new File(configFilePath);
            if (configFile.exists() && configFile.isFile()) {
                Properties configProperties = new Properties();
                try (FileInputStream is = new FileInputStream(configFile)) {
                    configProperties.load(is);
                }
                for (Map.Entry<Object, Object> entries : configProperties.entrySet()) {
                    res.getConfig().put(entries.getKey().toString(), entries.getValue().toString());
                }
            }
            return res;
        } catch (Exception ex) {
            throw new StepCliExecutionException("Unable to read config file: " + configFilePath, ex);
        }
    }

    // this example implements Callable, so parsing, error handling and handling user
    // requests for usage help or version help can be done with one line of code.
    public static void main(String... args) {
        // TODO: remove logging
        log.info("args: " + Arrays.asList(args));
        int exitCode = new CommandLine(new AutomationPackageConsole()).execute(args);
        System.exit(exitCode);
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setApFile(String apFile) {
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

    public void setAsync(String async) {
        this.async = async;
    }

    public void setApDeployHandler(CliCommandHandler apDeployHandler) {
        this.apDeployHandler = apDeployHandler;
    }
}
