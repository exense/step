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
import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.cli.parameters.ApExecuteParameters;
import step.cli.reports.AggregatedReportCreator;
import step.cli.reports.JUnitReportCreator;
import step.cli.reports.ReportCreator;
import step.client.executions.RemoteExecutionManager;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.IsolatedAutomationPackageExecutionParameters;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.plans.PlanFilter;
import step.core.plans.filters.*;
import step.core.plans.runner.PlanRunnerResult;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ExecuteAutomationPackageTool extends AbstractCliTool<ApExecuteParameters> {

    private static final Logger logger = LoggerFactory.getLogger(ExecuteAutomationPackageTool.class);

    public ExecuteAutomationPackageTool(String url, ApExecuteParameters params) {
        super(url, params);
    }

    public static String getExecutionTreeAsString(PlanRunnerResult res) {
        String executionTree;
        Writer w = new StringWriter();
        try {
            res.printTree(w, true, true);
            executionTree = w.toString();
        } catch (IOException e) {
            logger.error("Error while writing execution tree: {}", w);
            executionTree = "Error while writing tree. See logs for details.";
        }
        return executionTree;
    }

    public void execute() throws StepCliExecutionException {
        executePackageOnStep();
    }

    protected void executePackageOnStep() throws StepCliExecutionException {
        parameters.validate();

        File outputFolder = null;

        // prepare folders to store requested reports
        if (parameters.getReports() != null && !parameters.getReports().isEmpty()) {
            if (!parameters.getWaitForExecution()) {
                throw new StepCliExecutionException("The execution report can only been prepared in synchronous mode");
            }

            // even if the 'file' output mode is not defined, we create some output folder to store (temporarily) the report
            // because otherwise we will have no location to store the junit report before we print it to console in 'stdout' mode
            if (parameters.getReportOutputDir() == null) {
                outputFolder = new File(new File("").getAbsolutePath());
            } else {
                outputFolder = parameters.getReportOutputDir();
            }
            if (outputFolder.exists()) {
                if (!outputFolder.isDirectory()) {
                    throw new StepCliExecutionException("Report cannot be generated. Invalid folder: " + outputFolder.getAbsolutePath());
                }
            } else {
                boolean dirCreated = outputFolder.mkdir();
                if (!dirCreated) {
                    throw new StepCliExecutionException("Report cannot be generated. Folder hasn't been created: " + outputFolder.getAbsolutePath());
                }
            }

        }

        try (RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClient();
             RemoteExecutionManager remoteExecutionManager = createRemoteExecutionManager()) {
            IsolatedAutomationPackageExecutionParameters executionParameters = prepareExecutionParameters();

            List<String> executionIds;
            try {
                executionIds = automationPackageClient.executeAutomationPackage(
                        createPackageSource(parameters.getAutomationPackageFile(), createMavenArtifactXml(parameters.getAutomationPackageMavenArtifact())),
                        executionParameters,
                        createLibrarySource(parameters.getLibraryFile(), createMavenArtifactXml(parameters.getLibraryMavenArtifact())
                        , parameters.getManagedLibraryName()));
            } catch (AutomationPackageClientException e) {
                throw new StepCliExecutionException("Error while executing automation package: " + e.getMessage());
            }
            if (executionIds != null) {
                Map<String, Execution> executionInfos = new HashMap<>();
                logInfo("Execution(s) started in Step:", null);
                for (String executionId : executionIds) {
                    Execution executionInfo = remoteExecutionManager.get(executionId);
                    executionInfos.put(executionId, executionInfo);
                    logInfo("- " + executionToString(executionId, executionInfo), null);
                }

                if (parameters.getWaitForExecution()) {
                    logInfo("Waiting for execution(s) to complete...", null);

                    Exception executionError = null;
                    try {
                        waitForExecutionFinish(remoteExecutionManager, executionIds);
                    } catch (Exception ex){
                        // if some execution fails, we will get exception here, but we want to save the execution report anyway
                        executionError = ex;
                    }

                    if (parameters.getReports() != null && !executionIds.isEmpty()) {
                        try {
                            for (Report report : parameters.getReports()) {
                                ReportCreator reportCreator;
                                ReportType reportType = report.getReportType();
                                switch (reportType) {
                                    case junit:
                                        reportCreator = new JUnitReportCreator(remoteExecutionManager, outputFolder);
                                        break;
                                    case aggregated:
                                        reportCreator = new AggregatedReportCreator(remoteExecutionManager, outputFolder);
                                        break;
                                    default:
                                        throw new UnsupportedOperationException("Unsupported report type: " + reportType);
                                }
                                reportCreator.createReport(executionInfos, report.getOutputModes(), this);
                            }

                        } catch (Exception ex) {
                            logError("The execution report cannot be saved", ex);
                        }
                    }

                    // throw the original exception if exists
                    if (executionError != null) {
                        throw executionError;
                    }
                } else {
                    logInfo("waitForExecution set to 'false'. Not waiting for executions to complete.", null);
                }
            } else {
                throw logAndThrow("Unexpected response from Step. No execution Id returned. Please check the controller logs.");
            }
        } catch (StepCliExecutionException e) {
            throw e;
        } catch (Exception ex) {
            throw logAndThrow("Unexpected error while executing automation package", ex);
        }
    }

    protected boolean useLocalArtifact() {
        return parameters.getAutomationPackageMavenArtifact() == null;
    }

    protected void waitForExecutionFinish(RemoteExecutionManager remoteExecutionManager, List<String> executionIds) throws StepCliExecutionException {
        // run the execution and wait until it is finished
        try {
            List<Execution> endedExecutions = remoteExecutionManager.waitForTermination(executionIds, parameters.getExecutionResultTimeoutS() * 1000);
            int executionFailureCount = 0;
            for (String id : executionIds) {
                Execution endedExecution = endedExecutions.stream().filter(e -> e.getId().toString().equals(id)).findFirst().orElse(null);
                if (endedExecution == null) {
                    executionFailureCount++;
                    logError("Unknown result status for execution " + executionToString(id, null), null);
                } else if (!endedExecution.getImportResult().isSuccessful()) {
                    executionFailureCount++;
                    String errorMessage = "Error(s) while importing plan for execution " + executionToString(id, endedExecution);
                    List<String> errors = endedExecution.getImportResult().getErrors();
                    if (errors != null) {
                        errorMessage += ": " + String.join(";", errors);
                    }
                    logError(errorMessage, null);
                } else {
                    if (!isStatusSuccess(endedExecution)) {
                        executionFailureCount++;
                        String errorSummary = remoteExecutionManager.getFuture(id).getErrorSummary();
                        logError("Execution " + executionToString(id, endedExecution) + " failed. Result status was " + endedExecution.getResult() + ". Error summary: " + errorSummary, null);
                    } else {
                        logInfo("Execution " + executionToString(id, endedExecution) + " succeeded. Result status was " + endedExecution.getResult(), null);
                    }
                }
            }
            if (executionFailureCount > 0 && parameters.getEnsureExecutionSuccess()) {
                int executionsCount = executionIds.size();
                throw logAndThrow(executionFailureCount + "/" + executionsCount + " execution(s) failed. See " + getUrl() + "#/executions/list");
            }
        } catch (TimeoutException | InterruptedException ex) {
            throw logAndThrow("Timeout after " + parameters.getExecutionResultTimeoutS() + " seconds while waiting for executions to complete", ex);
        } catch (StepCliExecutionException e) {
            // Rethrow MojoExecutionException
            throw e;
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while executing automation package", e);
        }
    }

    private boolean isStatusSuccess(Execution ex){
        Set<ReportNodeStatus> okStatus = Set.of(ReportNodeStatus.PASSED, ReportNodeStatus.SKIPPED, ReportNodeStatus.NORUN);
        return okStatus.contains(ex.getResult());
    }

    private String executionToString(String id, Execution ex) {
        if (ex != null) {
            return String.format("'%s' (%s)", ex.getDescription(), getUrl() + "#/executions/" + ex.getId().toString());
        } else {
            return id;
        }
    }

    protected RemoteExecutionManager createRemoteExecutionManager() {
        RemoteExecutionManager remoteExecutionManager = new RemoteExecutionManager(getControllerCredentials());
        addProjectHeaderToRemoteClient(remoteExecutionManager);
        return remoteExecutionManager;
    }

    protected IsolatedAutomationPackageExecutionParameters prepareExecutionParameters() throws StepCliExecutionException {
        IsolatedAutomationPackageExecutionParameters executionParameters = new IsolatedAutomationPackageExecutionParameters();
        executionParameters.setMode(ExecutionMode.RUN);
        executionParameters.setCustomParameters(parameters.getExecutionParameters());
        executionParameters.setUserID(parameters.getUserId());

        executionParameters.setPlanFilter(getPlanFilters(parameters.getIncludePlans(), parameters.getExcludePlans(), parameters.getIncludeCategories(), parameters.getExcludeCategories()));
        executionParameters.setWrapIntoTestSet(parameters.getWrapIntoTestSet());
        executionParameters.setNumberOfThreads(parameters.getNumberOfThreads());

        return executionParameters;
    }

     public static PlanFilter getPlanFilters(String includePlans, String excludePlans, String includeCategories, String excludeCategories) {
        List<PlanFilter> multiFilter = new ArrayList<>();
        if (includePlans != null) {
            multiFilter.add(new PlanByIncludedNamesFilter(parseList(includePlans)));
        }
        if (excludePlans != null) {
            multiFilter.add(new PlanByExcludedNamesFilter(parseList(excludePlans)));
        }
        if (includeCategories != null) {
            multiFilter.add(new PlanByIncludedCategoriesFilter(parseList(includeCategories)));
        }
        if (excludeCategories != null) {
            multiFilter.add(new PlanByExcludedCategoriesFilter(parseList(excludeCategories)));
        }
        return new PlanMultiFilter(multiFilter);
    }

    private static List<String> parseList(String string) {
        return (string != null && !string.isBlank()) ? Arrays.stream(string.split(",")).collect(Collectors.toList()) : new ArrayList<>();
    }

    public enum ReportType {
        junit,
        aggregated
    }

    public enum ReportOutputMode {
        file,
        stdout
    }

    public static class Report {

        private final ReportType reportType;
        private final List<ReportOutputMode> outputModes;

        public Report(ReportType reportType) {
            this.reportType = reportType;
            switch (reportType){
                case junit:
                    this.outputModes = List.of(ReportOutputMode.file);
                    break;
                default:
                    this.outputModes = List.of(ReportOutputMode.stdout);
                    break;
            }
        }

        public Report(ReportType reportType, List<ReportOutputMode> outputModes) {
            this.reportType = reportType;
            this.outputModes = outputModes;
        }

        public ReportType getReportType() {
            return reportType;
        }

        public List<ReportOutputMode> getOutputModes() {
            return outputModes;
        }
    }
}
