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
import step.cli.ExecuteAutomationPackageTool;
import step.cli.parameters.ApExecuteParameters;
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

    @Parameter(property = "step-execute-auto-packages.user-id", required = false)
    private String userId;
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

    @Parameter
    private LibraryConfiguration library;
    //Individual property required to override values via system properties
    @Parameter(property = "step-execute-auto-packages.library.groupId")
    private String libraryGroupId;
    @Parameter(property = "step-execute-auto-packages.library.artifactId")
    private String libraryArtifactId;
    @Parameter(property = "step-execute-auto-packages.library.version")
    private String libraryVersion;
    @Parameter(property = "step-execute-auto-packages.library.classifier")
    private String libraryClassifier;
    @Parameter(property = "step-execute-auto-packages.library.type")
    private String libraryType;
    @Parameter(property = "step-execute-auto-packages.library.path")
    private String libraryPath;
    @Parameter(property = "step-execute-auto-packages.library.managed")
    private String libraryManaged;

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

    @Parameter
    private List<ReportParam> reports;

    @Parameter(property = "step-execute-auto-packages.reports")
    private String reportsRaw;

    @Parameter(property = "step-execute-auto-packages.report-dir")
    private String reportDir;

    public static class ReportParam {
        private ExecuteAutomationPackageTool.ReportType type;
        private String output;

        public ExecuteAutomationPackageTool.ReportType getType() {
            return type;
        }

        public void setType(ExecuteAutomationPackageTool.ReportType type) {
            this.type = type;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }
    }

    //TODO refactoring required as duplicated in Deploy Mojo, but as we need to duplicate the class's fields for the property annotation this was postponed for now
    private LibraryConfiguration prepareLibrary() {
        // Merge flat properties into the library object,
        // letting the structured XML config take precedence
        if (library == null) {
            library = new LibraryConfiguration();
        }
        if (libraryGroupId != null)    library.setGroupId(libraryGroupId);
        if (libraryArtifactId != null) library.setArtifactId(libraryArtifactId);
        if (libraryVersion != null)    library.setVersion(libraryVersion);
        if (libraryClassifier != null) library.setClassifier(libraryClassifier);
        if (libraryType != null)       library.setType(libraryType);
        if (libraryPath != null)       library.setPath(libraryPath);
        if (libraryManaged != null)    library.setManaged(libraryManaged);
        return library;
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            validateEEConfiguration(getStepProjectName(), getAuthToken());
            checkStepControllerVersion();
            library = prepareLibrary();
            if (library.isSet()) {
                library.validate();
            }

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

            List<ExecuteAutomationPackageTool.Report> parsedReports = parseReports();

            // if groupId and artifactId are not defined, we execute the maven artifact from current project
            File localApFile = null;
            if (remoteMavenArtifact == null) {
                Artifact applicableArtifact = getProjectArtifact(getArtifactClassifier());

                if (applicableArtifact != null) {
                    localApFile = applicableArtifact.getFile();
                    if (localApFile == null || !localApFile.exists()) {
                        throw logAndThrow("The resolved artifact '" + artifactToString(applicableArtifact) + "' contains no file.");
                    }
                } else {
                    throw logAndThrow("Unable to resolve automation package file " + artifactToString(project.getGroupId(), project.getArtifactId(), getArtifactClassifier(), project.getVersion()));
                }
            }

            File libraryFile = library != null ? library.toFile() : null;
            MavenArtifactIdentifier libraryMavenArtifact = library != null ? library.toMavenArtifactIdentifier() : null;
            String libraryName = library != null && library.isManagedLibraryNameConfigured() ? library.getManaged() : null;

            ApExecuteParameters params = new ApExecuteParameters()
                    .setAutomationPackageFile(localApFile)
                    .setAutomationPackageMavenArtifact(remoteMavenArtifact)
                    .setLibraryFile(libraryFile)
                    .setlibraryMavenArtifact(libraryMavenArtifact)
                    .setManagedLibraryName(libraryName)
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
                    .setReportOutputDir(reportOutputDir);

            createTool(getUrl(), params).execute();
        } catch (StepCliExecutionException e) {
            throw new MojoExecutionException("Execution exception", e);
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while uploading automation package to Step", e);
        }
    }

    private List<ExecuteAutomationPackageTool.Report> parseReports() {
        List<ReportParam> resolvedReports = getReports();
        resolvedReports = (resolvedReports == null || resolvedReports.isEmpty()) ? parseReports(reportsRaw) : resolvedReports;
        if (resolvedReports != null) {
            List<ExecuteAutomationPackageTool.Report> result = new ArrayList<>();
            for (ReportParam report : resolvedReports) {
                if (report.getOutput() == null || report.getOutput().isEmpty()) {
                    result.add(new ExecuteAutomationPackageTool.Report(report.getType()));
                } else {
                    List<ExecuteAutomationPackageTool.ReportOutputMode> outputModes = Arrays.stream(report.getOutput().split(","))
                            .map(ExecuteAutomationPackageTool.ReportOutputMode::valueOf)
                            .collect(Collectors.toList());
                    result.add(new ExecuteAutomationPackageTool.Report(report.getType(), outputModes));
                }
            }
            return result;
        } else {
            return null;
        }
    }

    protected List<ReportParam> parseReports(String raw) {
        if (raw == null || raw.isBlank()) return null;

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(entry -> {
                    String[] parts = entry.split(":", 2);
                    if (parts.length != 2) {
                        throw new IllegalArgumentException(
                                "Invalid report format '" + entry + "', expected 'type:outputPath'");
                    }
                    ReportParam report = new ReportParam();
                    report.setType(ExecuteAutomationPackageTool.ReportType.valueOf(parts[0].trim()));
                    report.setOutput(parts[1].trim());
                    return report;
                })
                .collect(Collectors.toList());
    }

    protected ExecuteAutomationPackageTool createTool(final String url, ApExecuteParameters params) {
        return new ExecuteAutomationPackageTool(url, params);
    }

    protected boolean isLocalMavenArtifact() {
        return getArtifactId() == null || getArtifactId().isEmpty() || getArtifactGroupId() == null || getArtifactGroupId().isEmpty();
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public LibraryConfiguration getLibrary() {
        return library;
    }

    public void setLibrary(LibraryConfiguration library) {
        this.library = library;
    }
}
