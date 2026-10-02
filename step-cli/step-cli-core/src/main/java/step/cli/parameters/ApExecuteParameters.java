package step.cli.parameters;

import step.cli.ExecuteAutomationPackageTool;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ApExecuteParameters extends ApParameters<ApExecuteParameters> {
    private String userId;
    private Map<String, String> executionParameters;
    private Integer executionResultTimeoutS;
    private Boolean waitForExecution;
    private Boolean ensureExecutionSuccess;

    private List<String> includePlans;
    private List<String> excludePlans;
    private Boolean wrapIntoTestSet;
    private Integer numberOfThreads;

    private List<String> includeCategories;
    private List<String> excludeCategories;

    private List<ExecuteAutomationPackageTool.Report> reports;
    private File reportOutputDir;

    public String getUserId() {
        return userId;
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

    public List<String> getIncludePlans() {
        return includePlans;
    }

    public List<String> getExcludePlans() {
        return excludePlans;
    }

    public List<String> getIncludeCategories() {
        return includeCategories;
    }

    public List<String> getExcludeCategories() {
        return excludeCategories;
    }


    public Boolean getWrapIntoTestSet() {
        return wrapIntoTestSet;
    }

    public Integer getNumberOfThreads() {
        return numberOfThreads;
    }

    public File getReportOutputDir() {
        return reportOutputDir;
    }

    public List<ExecuteAutomationPackageTool.Report> getReports() {
        return reports;
    }

    public ApExecuteParameters setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public ApExecuteParameters setExecutionParameters(Map<String, String> executionParameters) {
        this.executionParameters = executionParameters;
        return this;
    }

    public ApExecuteParameters setExecutionResultTimeoutS(Integer executionResultTimeoutS) {
        this.executionResultTimeoutS = executionResultTimeoutS;
        return this;
    }

    public ApExecuteParameters setWaitForExecution(Boolean waitForExecution) {
        this.waitForExecution = waitForExecution;
        return this;
    }

    public ApExecuteParameters setEnsureExecutionSuccess(Boolean ensureExecutionSuccess) {
        this.ensureExecutionSuccess = ensureExecutionSuccess;
        return this;
    }

    public ApExecuteParameters setIncludePlans(List<String> includePlans) {
        this.includePlans = includePlans;
        return this;
    }

    public ApExecuteParameters setExcludePlans(List<String> excludePlans) {
        this.excludePlans = excludePlans;
        return this;
    }

    public ApExecuteParameters setWrapIntoTestSet(Boolean wrapIntoTestSet) {
        this.wrapIntoTestSet = wrapIntoTestSet;
        return this;
    }

    public ApExecuteParameters setNumberOfThreads(Integer numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    public ApExecuteParameters setIncludeCategories(List<String> includeCategories) {
        this.includeCategories = includeCategories;
        return this;
    }

    public ApExecuteParameters setExcludeCategories(List<String> excludeCategories) {
        this.excludeCategories = excludeCategories;
        return this;
    }

    public ApExecuteParameters setReportOutputDir(File reportOutputDir) {
        this.reportOutputDir = reportOutputDir;
        return this;
    }

    public ApExecuteParameters setReports(List<ExecuteAutomationPackageTool.Report> reports) {
        this.reports = reports;
        return this;
    }
}
