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

import ch.exense.commons.io.FileHelper;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.client.executions.RemoteExecutionManager;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.AutomationPackageExecutionParameters;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.plans.PlanFilter;
import step.core.plans.filters.*;
import step.core.plans.runner.PlanRunnerResult;
import step.core.repositories.RepositoryObjectReference;
import step.reports.CustomReportType;
import step.repositories.ArtifactRepositoryConstants;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public abstract class AbstractExecuteAutomationPackageTool extends AbstractCliTool {

    private static final Logger logger = LoggerFactory.getLogger(AbstractExecuteAutomationPackageTool.class);
    private final Params params;

    public AbstractExecuteAutomationPackageTool(String url, Params params) {
        super(url);
        this.params = params;
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
        File outputFolder = null;

        // prepare folders to store requested reports
        if (params.getReportTypes() != null && !params.getReportTypes().isEmpty()) {
            if (!params.getWaitForExecution()) {
                throw new StepCliExecutionException("The execution report can only been prepared in synchronous mode");
            }
            if (params.getReportOutputDir() == null) {
                outputFolder = new File("");
            } else {
                outputFolder = params.getReportOutputDir();
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
            File automationPackageFile = null;

            // if group id and artifact id are specified, this means, that don't want to send the artifact (binary) to the controller,
            if (useLocalArtifact()) {
                automationPackageFile = getAutomationPackageFile();
            }

            AutomationPackageExecutionParameters executionParameters = prepareExecutionParameters();

            List<String> executionIds;
            try {
                executionIds = automationPackageClient.executeAutomationPackage(automationPackageFile, executionParameters);
            } catch (AutomationPackageClientException e) {
                throw logAndThrow("Error while executing automation package: " + e.getMessage());
            }
            if (executionIds != null) {
                logInfo("Execution(s) started in Step:", null);
                for (String executionId : executionIds) {
                    Execution executionInfo = remoteExecutionManager.get(executionId);
                    logInfo("- " + executionToString(executionId, executionInfo), null);
                }

                if (params.getWaitForExecution()) {
                    logInfo("Waiting for execution(s) to complete...", null);

                    Exception executionError = null;
                    try {
                        waitForExecutionFinish(remoteExecutionManager, executionIds);
                    } catch (Exception ex){
                        // if some execution fails, we will get exception here, but we want to save the execution report anyway
                        executionError = ex;
                    }

                    if (params.getReportTypes() != null && !executionIds.isEmpty()) {
                        try {
                            for (ReportType reportType : params.getReportTypes()) {
                                CustomReportType serverReportType = null;
                                switch (reportType){
                                    case junit:
                                        serverReportType = CustomReportType.JUNITZIP;
                                        break;
                                    default:
                                        throw new UnsupportedOperationException("Unsupported report type: " + reportType);
                                }

                                // only for zipped reports we want to include attachments
                                Boolean includeAttachments = serverReportType == CustomReportType.JUNITZIP;

                                RemoteExecutionManager.Report customReport;
                                File outputFile;

                                Path currentFolder = new File("").toPath().toAbsolutePath();
                                Path relativePathToOutputDir = currentFolder.relativize(params.getReportOutputDir().toPath().toAbsolutePath());
                                // report output directory is sent as a root folder for attachments
                                if (executionIds.size() > 1) {
                                    customReport = remoteExecutionManager.getCustomMultiReport(executionIds, serverReportType, includeAttachments, relativePathToOutputDir.toFile().getPath());
                                    outputFile = new File(outputFolder, customReport.getFileName());
                                } else {
                                    customReport = remoteExecutionManager.getCustomReport(executionIds.get(0), serverReportType, includeAttachments, relativePathToOutputDir.toFile().getPath());
                                    outputFile = new File(outputFolder, customReport.getFileName());
                                }
                                logInfo("Saving execution report (" + params.getReportTypes() + ") into " + outputFile.getAbsolutePath(), null);

                                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                                    fos.write(customReport.getContent());
                                }

                                // automatically unzip file
                                if (serverReportType == CustomReportType.JUNITZIP) {
                                    File folderToUnzip = new File(outputFolder, Files.getNameWithoutExtension(outputFile.getName()));
                                    if (!folderToUnzip.exists()) {
                                        folderToUnzip.mkdir();
                                    }
                                    logInfo("Unzip the report into " + folderToUnzip, null);
                                    FileHelper.unzip(outputFile, folderToUnzip);
                                    boolean deleted = outputFile.delete();
                                    if (!deleted) {
                                        logInfo("File cannot be deleted: " + outputFile.getAbsolutePath(), null);
                                    }
                                }
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
        return params.getMavenArtifactIdentifier() == null;
    }

    protected void waitForExecutionFinish(RemoteExecutionManager remoteExecutionManager, List<String> executionIds) throws StepCliExecutionException {
        // run the execution and wait until it is finished
        try {
            List<Execution> endedExecutions = remoteExecutionManager.waitForTermination(executionIds, params.getExecutionResultTimeoutS() * 1000);
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
                } else if (!isStatusSuccess(endedExecution)) {
                    executionFailureCount++;
                    String errorSummary = remoteExecutionManager.getFuture(id).getErrorSummary();
                    logError("Execution " + executionToString(id, endedExecution) + " failed. Result status was " + endedExecution.getResult() + ". Error summary: " + errorSummary, null);
                } else {
                    logInfo("Execution " + executionToString(id, endedExecution) + " succeeded. Result status was " + endedExecution.getResult(), null);
                }
            }
            if (executionFailureCount > 0 && params.getEnsureExecutionSuccess()) {
                int executionsCount = executionIds.size();
                throw logAndThrow(executionFailureCount + "/" + executionsCount + " execution(s) failed. See " + getUrl() + "#/executions/list");
            }
        } catch (TimeoutException | InterruptedException ex) {
            throw logAndThrow("Timeout after " + params.getExecutionResultTimeoutS() + " seconds while waiting for executions to complete", ex);
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

    protected abstract File getAutomationPackageFile() throws StepCliExecutionException;

    @Override
    protected ControllerCredentials getControllerCredentials() {
        String authToken = params.getAuthToken();
        return new ControllerCredentials(getUrl(), authToken == null || authToken.isEmpty() ? null : authToken);
    }

    protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
        RemoteAutomationPackageClientImpl client = new RemoteAutomationPackageClientImpl(getControllerCredentials());
        addProjectHeaderToRemoteClient(client);
        return client;
    }

    protected RemoteExecutionManager createRemoteExecutionManager() {
        RemoteExecutionManager remoteExecutionManager = new RemoteExecutionManager(getControllerCredentials());
        addProjectHeaderToRemoteClient(remoteExecutionManager);
        return remoteExecutionManager;
    }

    private void addProjectHeaderToRemoteClient(AbstractRemoteClient remoteClient) {
        addProjectHeaderToRemoteClient(params.getStepProjectName(), remoteClient);
    }

    protected AutomationPackageExecutionParameters prepareExecutionParameters() throws StepCliExecutionException {
        AutomationPackageExecutionParameters executionParameters = new AutomationPackageExecutionParameters();
        executionParameters.setMode(ExecutionMode.RUN);
        executionParameters.setCustomParameters(params.getExecutionParameters());
        executionParameters.setUserID(params.getUserId());

        executionParameters.setPlanFilter(getPlanFilters(params.getIncludePlans(), params.getExcludePlans(), params.getIncludeCategories(), params.getExcludeCategories()));
        executionParameters.setWrapIntoTestSet(params.getWrapIntoTestSet());
        executionParameters.setNumberOfThreads(params.getNumberOfThreads());

        if (params.getMavenArtifactIdentifier() != null) {
            Map<String, String> repositoryParameters = new HashMap<>();
            repositoryParameters.put(ArtifactRepositoryConstants.ARTIFACT_PARAM_ARTIFACT_ID, params.getMavenArtifactIdentifier().getArtifactId());
            repositoryParameters.put(ArtifactRepositoryConstants.ARTIFACT_PARAM_GROUP_ID, params.getMavenArtifactIdentifier().getGroupId());
            repositoryParameters.put(ArtifactRepositoryConstants.ARTIFACT_PARAM_VERSION, params.getMavenArtifactIdentifier().getVersion());
            repositoryParameters.put(ArtifactRepositoryConstants.ARTIFACT_PARAM_CLASSIFIER, params.getMavenArtifactIdentifier().getClassifier());
            executionParameters.setOriginalRepositoryObject(new RepositoryObjectReference(ArtifactRepositoryConstants.MAVEN_REPO_ID, repositoryParameters));
        }

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

    public static class Params {
        private String stepProjectName;
        private String userId;
        private String authToken;

        private Map<String, String> executionParameters;
        private Integer executionResultTimeoutS;
        private Boolean waitForExecution;
        private Boolean ensureExecutionSuccess;

        private String includePlans;
        private String excludePlans;
        private Boolean wrapIntoTestSet;
        private Integer numberOfThreads;
        private MavenArtifactIdentifier mavenArtifactIdentifier;
        private String includeCategories;
        private String excludeCategories;

        private List<ReportType> reportTypes;
        private File reportOutputDir;

        public String getStepProjectName() {
            return stepProjectName;
        }

        public String getUserId() {
            return userId;
        }

        public String getAuthToken() {
            return authToken;
        }

        public Map<String, String> getExecutionParameters() {
            return executionParameters;
        }

        public Integer getExecutionResultTimeoutS() {
            return executionResultTimeoutS;
        }

        public Boolean getWaitForExecution() {
            return waitForExecution;
        }

        public Boolean getEnsureExecutionSuccess() {
            return ensureExecutionSuccess;
        }

        public String getIncludePlans() {
            return includePlans;
        }

        public String getExcludePlans() {
            return excludePlans;
        }

        public String getIncludeCategories() {
            return includeCategories;
        }

        public String getExcludeCategories() {
            return excludeCategories;
        }

        public MavenArtifactIdentifier getMavenArtifactIdentifier() {
            return mavenArtifactIdentifier;
        }

        public Boolean getWrapIntoTestSet() {
            return wrapIntoTestSet;
        }

        public Integer getNumberOfThreads() {
            return numberOfThreads;
        }

        public List<ReportType> getReportTypes() {
            return reportTypes;
        }

        public File getReportOutputDir() {
            return reportOutputDir;
        }

        public Params setStepProjectName(String stepProjectName) {
            this.stepProjectName = stepProjectName;
            return this;
        }

        public Params setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Params setAuthToken(String authToken) {
            this.authToken = authToken;
            return this;
        }

        public Params setExecutionParameters(Map<String, String> executionParameters) {
            this.executionParameters = executionParameters;
            return this;
        }

        public Params setExecutionResultTimeoutS(Integer executionResultTimeoutS) {
            this.executionResultTimeoutS = executionResultTimeoutS;
            return this;
        }

        public Params setWaitForExecution(Boolean waitForExecution) {
            this.waitForExecution = waitForExecution;
            return this;
        }

        public Params setEnsureExecutionSuccess(Boolean ensureExecutionSuccess) {
            this.ensureExecutionSuccess = ensureExecutionSuccess;
            return this;
        }

        public Params setIncludePlans(String includePlans) {
            this.includePlans = includePlans;
            return this;
        }

        public Params setExcludePlans(String excludePlans) {
            this.excludePlans = excludePlans;
            return this;
        }

        public Params setWrapIntoTestSet(Boolean wrapIntoTestSet) {
            this.wrapIntoTestSet = wrapIntoTestSet;
            return this;
        }

        public Params setNumberOfThreads(Integer numberOfThreads) {
            this.numberOfThreads = numberOfThreads;
            return this;
        }

        public Params setMavenArtifactIdentifier(MavenArtifactIdentifier mavenArtifactIdentifier) {
            this.mavenArtifactIdentifier = mavenArtifactIdentifier;
            return this;
        }

        public Params setIncludeCategories(String includeCategories) {
            this.includeCategories = includeCategories;
            return this;
        }

        public Params setExcludeCategories(String excludeCategories) {
            this.excludeCategories = excludeCategories;
            return this;
        }

        public Params setReportTypes(List<ReportType> reportTypes) {
            this.reportTypes = reportTypes;
            return this;
        }

        public Params setReportOutputDir(File reportOutputDir) {
            this.reportOutputDir = reportOutputDir;
            return this;
        }
    }

    public enum ReportType {
        junit
    }
}
