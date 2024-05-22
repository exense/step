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
import java.util.Arrays;
import java.util.concurrent.Callable;

@Command(name = "manageAP", mixinStandardHelpOptions = true, version = "manageAP 1.0",
        description = "Deploys or executes the automation package")
public class AutomationPackageConsole implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageConsole.class);

    @Parameters(index = "0", description = "\"Deploy\" or \"Execute\"")
    private String command;

    @Parameters(index = "1", description = "The file with automation package")
    private File file;

    @Option(names = {"--url"}, required = true)
    private String url;

    @Option(names = {"--groupId"})
    private String groupId;

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

    @Option(names = {"--async"})
    private Boolean async;

    @Override
    public Integer call() throws Exception {
        switch (command.toLowerCase()) {
            case "deploy":
                new AbstractDeployAutomationPackageTool(url, groupId, artifactId, artifactVersion, artifactClassifier, stepProjectName, authToken, async) {
                    @Override
                    protected File getFileToUpload() throws StepCliExecutionException {
                        return file;
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
                break;
            default:
                System.out.println("Unknown command: " + command);
                return -1;
        }
        return 0;
    }

    // this example implements Callable, so parsing, error handling and handling user
    // requests for usage help or version help can be done with one line of code.
    public static void main(String... args) {
        // TODO: remove logging
        log.info("args: " + Arrays.asList(args));
        int exitCode = new CommandLine(new AutomationPackageConsole()).execute(args);
        System.exit(exitCode);
    }
}
