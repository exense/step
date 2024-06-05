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

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import step.automation.packages.AutomationPackageFromInputStreamProvider;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.junit.AbstractLocalPlanRunner;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.Artefact;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.junit.runner.StepClassParserResult;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static step.cli.Parameters.CONFIG;

@Command(name = "step", mixinStandardHelpOptions = true, version = "step 1.0",
        description = "The CLI interface to communicate with Step server", defaultValueProvider = StepDefaultValuesProvider.class)
public class StepConsole implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(StepConsole.class);

    @Parameters(index = "0", description = "\"Deploy\" or \"Execute\" or \"RunTests\"")
    private String command;

    @Option(names = {"--" + CONFIG}, description = "The custom configuration file")
    private String config;

    @Option(names = {"--apFile"}, description = "The file with automation package")
    private File apFile;

    @Option(names = {"--stepUrl"})
    private String stepUrl;

    @Option(names = {"-ep", "--executionParameters"}, description = "The execution parameters to be used in \"Execute\" or \"RunTests\"")
    protected Map<String, String> executionParameters;

    @CommandLine.ArgGroup(validate = false, heading = "The security parameters (for Step EE only)%n")
    protected SecurityParams securityParams = new SecurityParams();

    @CommandLine.ArgGroup(validate = false, heading = "The parameters for AP deployment%n")
    protected ApDeployParams apDeployParams = new ApDeployParams();

    @CommandLine.ArgGroup(validate = false, heading = "The parameters for AP execution%n")
    protected ApExecuteParams apExecuteParams = new ApExecuteParams();

    @CommandLine.ArgGroup(validate = false, heading = "The parameters to run tests for AP%n")
    protected ApRunTestsParams apRunTestsParams = new ApRunTestsParams();

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

    protected static class ApRunTestsParams {
        @Option(names = {"--test-plans"})
        protected List<String> testPlans;
    }

    protected static class ApExecuteParams {
        @Option(names = {"--executionTimeoutS"}, defaultValue = "3600")
        protected Integer executionTimeoutS;

        @Option(names = {"--waitForExecution"}, defaultValue = "true")
        protected Boolean waitForExecution;

        @Option(names = {"--ensureExecutionSuccess"}, defaultValue = "true")
        protected Boolean ensureExecutionSuccess;

        @Option(names = {"--includePlans"})
        protected String includePlans;

        @Option(names = {"--excludePlans"})
        protected String excludePlans;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new StepConsole()).execute(args);
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
            case "runtests":
                handleRunTestsCommand();
                break;
            default:
                log.error("Unknown command: " + command);
                return -1;
        }
        return 0;
    }

    private void handleRunTestsCommand() {
        try (ExecutionEngine executionEngine = ExecutionEngine.builder().withPlugin(new AbstractExecutionEnginePlugin() {
            @Override
            public void afterExecutionEnd(ExecutionContext context) {
                super.afterExecutionEnd(context);
            }
        }).withPluginsFromClasspath().build()) {
            AutomationPackageManager automationPackageManager = executionEngine.getExecutionEngineContext().require(AutomationPackageManager.class);

            File file = prepareApFile(apFile);
            if (file == null) {
                throw new StepCliExecutionException("AP file is not defined");
            }

            try (InputStream is = new FileInputStream(file)) {
                AutomationPackageFromInputStreamProvider automationPackageProvider = new AutomationPackageFromInputStreamProvider(is, apFile.getName());
                ObjectId automationPackageId = automationPackageManager.createOrUpdateAutomationPackage(
                        false, true, null, automationPackageProvider,
                        true, null, null, false
                ).getId();

                // TODO: apply filter for plans
                List<StepClassParserResult> listPlans = automationPackageManager.getPackagePlans(automationPackageId)
                        .stream()
                        .filter(p -> p.getRoot().getClass().getAnnotation(Artefact.class).validForStandaloneExecution())
                        .map(p -> new StepClassParserResult(p.getAttribute(AbstractOrganizableObject.NAME), p, null))
                        .collect(Collectors.toList());

                log.info("The following plans will be executed: {}", listPlans.stream().map(StepClassParserResult::getName).collect(Collectors.toList()));

                for (StepClassParserResult parserResult : listPlans) {
                    String planName = parserResult.getName();
                    new AbstractLocalPlanRunner() {
                        @Override
                        protected void onExecutionStart() {
                            log.info("Execution has been started for plan {}", planName);
                        }

                        @Override
                        protected void onExecutionError(PlanRunnerResult result, String errorText, boolean assertionError) {
                            log.error("Execution has been failed for plan {}. {}", planName, errorText);
                        }

                        @Override
                        protected void onInitializingException(Exception exception) {
                            log.error("Execution initialization exception for plan {}.", planName, exception);
                        }

                        @Override
                        protected void onExecutionException(Exception exception) {
                            log.error("Execution exception for plan {}", planName, exception);
                        }

                        @Override
                        protected void onTestFinished() {
                            log.info("Execution has been finished for plan {}", planName);
                        }

                        @Override
                        protected Map<String, String> getExecutionParameters() {
                            return executionParameters;
                        }
                    }.runPlan(parserResult, executionEngine);
                }
            } catch (FileNotFoundException e) {
                throw new StepCliExecutionException("File not found: " + apFile.getAbsolutePath(), e);
            } catch (IOException e) {
                throw new RuntimeException("IO exception for " + apFile.getAbsolutePath(), e);
            } catch (AutomationPackageReadingException e) {
                throw new RuntimeException("AP reading exception", e);
            }
        }
    }

    protected void handleExecuteCommand() {
        new AbstractExecuteAutomationPackageTool(
                stepUrl, securityParams.stepProjectName, securityParams.stepUserId, securityParams.authToken,
                executionParameters, apExecuteParams.executionTimeoutS,
                apExecuteParams.waitForExecution, apExecuteParams.ensureExecutionSuccess,
                apExecuteParams.includePlans, apExecuteParams.excludePlans
        ) {
            @Override
            protected File getAutomationPackageFile() throws StepCliExecutionException {
                return prepareApFile(apFile);
            }
        }.execute();
    }

    protected void handleDeployCommand() {
        new AbstractDeployAutomationPackageTool(stepUrl, securityParams.stepProjectName, securityParams.authToken, apDeployParams.async) {
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
        this.executionParameters = executionParameters;
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
}
