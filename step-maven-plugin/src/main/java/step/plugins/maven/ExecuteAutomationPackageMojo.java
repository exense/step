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
package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.cli.AbstractExecuteAutomationPackageTool;
import step.core.maven.MavenArtifactIdentifier;
import step.cli.StepCliExecutionException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mojo(name = "execute-automation-package")
public class ExecuteAutomationPackageMojo extends AbstractStepPluginMojo {

    @Parameter(property = "step.step-project-name", required = false)
    private String stepProjectName;
    @Parameter(property = "step-execute-auto-packages.user-id", required = false)
    private String userId;
    @Parameter(property = "step.auth-token", required = false)
    private String authToken;

    @Parameter(property = "step-execute-auto-packages.artifact-group-id")
    private String artifactGroupId;
    @Parameter(property = "step-execute-auto-packages.artifact-id")
    private String artifactId;
    @Parameter(property = "step-execute-auto-packages.artifact-version")
    private String artifactVersion;
    @Parameter(property = "step-execute-auto-packages.artifact-classifier", required = false)
    private String artifactClassifier;
    @Parameter(property = "step-execute-auto-packages.artifact-type", required = false)
    private String artifactType;

    @Parameter(property = "step-execute-auto-packages.lib-artifact-group-id")
    private String libArtifactGroupId;
    @Parameter(property = "step-execute-auto-packages.lib-artifact-id")
    private String libArtifactId;
    @Parameter(property = "step-execute-auto-packages.lib-artifact-version")
    private String libArtifactVersion;
    @Parameter(property = "step-execute-auto-packages.lib-artifact-classifier", required = false)
    private String libArtifactClassifier;
    @Parameter(property = "step-execute-auto-packages.lib-artifact-type", required = false)
    private String libArtifactType;

    @Parameter(property = "step-execute-auto-packages.execution-parameters", required = false)
    private Map<String, String> executionParameters;
    @Parameter(property = "step-execute-auto-packages.exec-result-timeout-s", defaultValue = "3600")
    private Integer executionResultTimeoutS;
    @Parameter(property = "step-execute-auto-packages.wait-for-exec", defaultValue = "true")
    private Boolean waitForExecution;
    @Parameter(property = "step-execute-auto-packages.ensure-exec-success", defaultValue = "true")
    private Boolean ensureExecutionSuccess;

    @Parameter(property = "step-execute-auto-packages.include-plans")
    private String includePlans;
    @Parameter(property = "step-execute-auto-packages.exclude-plans")
    private String excludePlans;

    @Parameter(property = "step-execute-auto-packages.include-categories")
    private String includeCategories;
    @Parameter(property = "step-execute-auto-packages.exclude-categories")
    private String excludeCategories;

    @Parameter(property = "step-execute-auto-packages.wrap-into-test-set", defaultValue = "false")
    private Boolean wrapIntoTestSet;
    @Parameter(property = "step-execute-auto-packages.number-of-threads")
    private Integer numberOfThreads;

    @Parameter(property = "step-execute-auto-packages.reports")
    private List<ReportParam> reports;

    @Parameter(property = "step-execute-auto-packages.report-dir")
    private String reportDir;

    public static class ReportParam {
        private AbstractExecuteAutomationPackageTool.ReportType type;
        private String output;

        public AbstractExecuteAutomationPackageTool.ReportType getType() {
            return type;
        }

        public void setType(AbstractExecuteAutomationPackageTool.ReportType type) {
            this.type = type;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            validateEEConfiguration(getStepProjectName(), getAuthToken());
            checkStepControllerVersion();

            MavenArtifactIdentifier remoteMavenArtifact = null;
            if (!isLocalMavenArtifact()) {
                remoteMavenArtifact = new MavenArtifactIdentifier(getArtifactGroupId(), getArtifactId(), getArtifactVersion(), getArtifactClassifier(), getArtifactType());
            }

            File reportOutputDir = null;
            if (getReports() != null && !getReports().isEmpty()) {
                String resolvedReportDir = getReportDir();
                if (resolvedReportDir == null) {
                    String targetDir = project.getBuild().getDirectory();
                    resolvedReportDir = targetDir + File.separator + "step-reports";
                }
                reportOutputDir = new File(resolvedReportDir);
            }

            List<AbstractExecuteAutomationPackageTool.Report> parsedReports = parseReports();

            AbstractExecuteAutomationPackageTool.Params params = new AbstractExecuteAutomationPackageTool.Params()
                    .setStepProjectName(getStepProjectName())
                    .setUserId(getUserId())
                    .setAuthToken(getAuthToken())
                    .setExecutionParameters(getExecutionParameters())
                    .setExecutionResultTimeoutS(getExecutionResultTimeoutS())
                    .setWaitForExecution(getWaitForExecution())
                    .setEnsureExecutionSuccess(getEnsureExecutionSuccess())
                    .setReports(parsedReports)
                    .setIncludePlans(getIncludePlans())
                    .setExcludePlans(getExcludePlans())
                    .setIncludeCategories(getIncludeCategories())
                    .setExcludeCategories(getExcludeCategories())
                    .setWrapIntoTestSet(getWrapIntoTestSet())
                    .setNumberOfThreads(getNumberOfThreads())
                    .setMavenArtifactIdentifier(remoteMavenArtifact)
                    .setReportOutputDir(reportOutputDir);

            createTool(getUrl(), params).execute();
        } catch (StepCliExecutionException e) {
            throw new MojoExecutionException("Execution exception", e);
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    private List<AbstractExecuteAutomationPackageTool.Report> parseReports() {
        if (getReports() != null) {
            List<AbstractExecuteAutomationPackageTool.Report> result = new ArrayList<>();
            for (ReportParam report : getReports()) {
                if (report.getOutput() == null || report.getOutput().isEmpty()) {
                    result.add(new AbstractExecuteAutomationPackageTool.Report(report.getType()));
                } else {
                    List<AbstractExecuteAutomationPackageTool.ReportOutputMode> outputModes = Arrays.stream(report.getOutput().split(","))
                            .map(AbstractExecuteAutomationPackageTool.ReportOutputMode::valueOf)
                            .collect(Collectors.toList());
                    result.add(new AbstractExecuteAutomationPackageTool.Report(report.getType(), outputModes));
                }
            }
            return result;
        } else {
            return null;
        }
    }

    protected AbstractExecuteAutomationPackageTool createTool(final String url, AbstractExecuteAutomationPackageTool.Params params) {
        // TODO: here we need to support the maven snippet for the library file
        return new AbstractExecuteAutomationPackageTool(url, params, null) {
            @Override
            protected File getAutomationPackageFile() throws StepCliExecutionException {
                // if groupId and artifactId are not defined, we execute the maven artifact from current project
                if (useLocalArtifact()) {
                    Artifact applicableArtifact = getProjectArtifact(getArtifactClassifier());

                    if (applicableArtifact != null) {
                        File artifactFile = applicableArtifact.getFile();
                        if(artifactFile == null || !artifactFile.exists()) {
                            throw logAndThrow("The resolved artifact '" + artifactToString(applicableArtifact) + "' contains no file.");
                        } else {
                            return artifactFile;
                        }
                    } else {
                        throw logAndThrow("Unable to resolve automation package file " + artifactToString(project.getGroupId(), project.getArtifactId(), getArtifactClassifier(), project.getVersion()));
                    }
                } else {
                    return null;
                }
            }
        };
    }

    protected MavenArtifactIdentifier getKeywordLibRemoteMavenIdentifier() throws MojoExecutionException {
        if(getLibArtifactId() != null && !getLibArtifactId().isEmpty() && getLibArtifactGroupId() != null && !getLibArtifactGroupId().isEmpty()){
            return new MavenArtifactIdentifier(getLibArtifactGroupId(), getLibArtifactId(), getLibArtifactVersion(), getLibArtifactClassifier(), getLibArtifactType());
        } else {
            return null;
        }
    }


    protected boolean isLocalMavenArtifact() {
        return getArtifactId() == null || getArtifactId().isEmpty() || getArtifactGroupId() == null || getArtifactGroupId().isEmpty();
    }

    public String getStepProjectName() {
        return stepProjectName;
    }

    public void setStepProjectName(String stepProjectName) {
        this.stepProjectName = stepProjectName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getArtifactGroupId() {
        return artifactGroupId;
    }

    public void setArtifactGroupId(String artifactGroupId) {
        this.artifactGroupId = artifactGroupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getArtifactClassifier() {
        return artifactClassifier;
    }

    public void setArtifactClassifier(String artifactClassifier) {
        this.artifactClassifier = artifactClassifier;
    }

    public Map<String, String> getExecutionParameters() {
        return executionParameters;
    }

    public void setExecutionParameters(Map<String, String> executionParameters) {
        this.executionParameters = executionParameters;
    }

    public Integer getExecutionResultTimeoutS() {
        return executionResultTimeoutS;
    }

    public void setExecutionResultTimeoutS(Integer executionResultTimeoutS) {
        this.executionResultTimeoutS = executionResultTimeoutS;
    }

    public Boolean getWaitForExecution() {
        return waitForExecution;
    }

    public void setWaitForExecution(Boolean waitForExecution) {
        this.waitForExecution = waitForExecution;
    }

    public Boolean getEnsureExecutionSuccess() {
        return ensureExecutionSuccess;
    }

    public void setEnsureExecutionSuccess(Boolean ensureExecutionSuccess) {
        this.ensureExecutionSuccess = ensureExecutionSuccess;
    }

    public List<ReportParam> getReports() {
        return reports;
    }

    public void setReports(List<ReportParam> reports) {
        this.reports = reports;
    }

    public String getIncludePlans() {
        return includePlans;
    }

    public void setIncludePlans(String includePlans) {
        this.includePlans = includePlans;
    }

    public String getExcludePlans() {
        return excludePlans;
    }

    public void setExcludePlans(String excludePlans) {
        this.excludePlans = excludePlans;
    }

    public String getIncludeCategories() {
        return includeCategories;
    }

    public void setIncludeCategories(String includeCategories) {
        this.includeCategories = includeCategories;
    }

    public String getExcludeCategories() {
        return excludeCategories;
    }

    public void setExcludeCategories(String excludeCategories) {
        this.excludeCategories = excludeCategories;
    }

    public Boolean getWrapIntoTestSet() {
        return wrapIntoTestSet;
    }

    public void setWrapIntoTestSet(Boolean wrapIntoTestSet) {
        this.wrapIntoTestSet = wrapIntoTestSet;
    }

    public Integer getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(Integer numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public String getReportDir() {
        return reportDir;
    }

    public void setReportDir(String reportDir) {
        this.reportDir = reportDir;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    public String getLibArtifactGroupId() {
        return libArtifactGroupId;
    }

    public void setLibArtifactGroupId(String libArtifactGroupId) {
        this.libArtifactGroupId = libArtifactGroupId;
    }

    public String getLibArtifactId() {
        return libArtifactId;
    }

    public void setLibArtifactId(String libArtifactId) {
        this.libArtifactId = libArtifactId;
    }

    public String getLibArtifactVersion() {
        return libArtifactVersion;
    }

    public void setLibArtifactVersion(String libArtifactVersion) {
        this.libArtifactVersion = libArtifactVersion;
    }

    public String getLibArtifactClassifier() {
        return libArtifactClassifier;
    }

    public void setLibArtifactClassifier(String libArtifactClassifier) {
        this.libArtifactClassifier = libArtifactClassifier;
    }

    public String getLibArtifactType() {
        return libArtifactType;
    }

    public void setLibArtifactType(String libArtifactType) {
        this.libArtifactType = libArtifactType;
    }
}
