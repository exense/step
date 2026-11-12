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
import step.cli.ExecuteAutomationPackageTool.Report;
import step.cli.StepConsoleLogging.Restorer;
import step.cli.reports.ReportRunFolder;
import step.agents.provisioning.local.LocalAgentProvisioningConfiguration;
import step.agents.provisioning.local.LocalAgentProvisioningPlugin;
import step.automation.packages.AutomationPackageFromInputStreamProvider;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.AutomationPackageUpdateParameter;
import step.automation.packages.AutomationPackageUpdateParameterBuilder;
import step.automation.packages.junit.AbstractLocalPlanRunner;
import step.automation.packages.library.AutomationPackageLibraryFromInputStreamProvider;
import step.automation.packages.library.AutomationPackageLibraryProvider;
import step.automation.packages.library.NoAutomationPackageLibraryProvider;
import step.artefacts.handlers.functions.AgentProvisioningExecutionPlugin;
import step.core.accessors.AbstractOrganizableObject;
import step.core.agents.provisioning.driver.AgentProvisioningStatusAccessor;
import step.core.artefacts.Artefact;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.execution.ExecutionEngine;
import step.core.execution.OperationMode;
import step.core.plans.Plan;
import step.core.plans.PlanFilter;
import step.core.plans.runner.PlanRunnerResult;
import step.junit.runner.StepClassParserResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static step.cli.ExecuteAutomationPackageTool.getPlanFilters;

public class ApLocalExecuteCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(ApLocalExecuteCommandHandler.class);

    /**
     * @param reports         the reports to produce, as chosen with --reportType
     * @param reportDir       the directory holding the folder of the run, the report files and the attachments are
     *                        written to
     * @param saveAttachments if the attachments of the report nodes are written to the folder of the run
     */
    public void execute(File apFile, File libFile, List<String> includePlans, List<String> excludePlans, List<String> includeCategories,
                        List<String> excludeCategories, Map<String, String> executionParameters,
                        LocalAgentProvisioningConfiguration localAgentConfiguration, List<Report> reports, File reportDir,
                        boolean saveAttachments) throws StepCliExecutionException {
        Objects.requireNonNull(reports);
        Objects.requireNonNull(reportDir);
        ReportRunFolder runFolder = new ReportRunFolder(reportDir);
        AttachmentExporter attachmentExporter = saveAttachments ? new AttachmentExporter(runFolder) : null;
        // The keywords run on real agents started as separate processes on this machine, so that a local execution
        // exercises the same agents and the same class loader isolation as an execution on a Step platform, and
        // supports every keyword language rather than only the Java ones.
        // Declared before the engine, and therefore closed after it: the local grid it stops is still in use as long
        // as the engine has not released the class loaders of its local tokens. The same goes for the file of the
        // live report, written to until the engine is closed, and for the attachments the live reporting server
        // stores, read until then.
        // The live reporting server receives the files the keywords stream, stored with the other attachments of the
        // nodes, as on a Step platform, for the reports to include them whether or not they are also saved.
        try (LocalReports localReports = new LocalReports(reports, runFolder, attachmentExporter);
             LocalLiveReportingServer liveReportingServer = startLiveReportingServer();
             LocalAgentProvisioningPlugin localAgents = new LocalAgentProvisioningPlugin(localAgentConfiguration);
             ExecutionEngine executionEngine = buildExecutionEngine(localAgents, localReports.openLiveReportOutputs(),
                 attachmentExporter, liveReportingServer)) {
            AutomationPackageManager automationPackageManager = executionEngine.getExecutionEngineContext().require(AutomationPackageManager.class);

            InputStream libFileInputStream = null;
            try (InputStream is = new FileInputStream(apFile)) {
                if (libFile != null) {
                    libFileInputStream = new FileInputStream(libFile);
                }
                AutomationPackageLibraryProvider libFromInputStreamProvider = libFileInputStream == null ?
                    new NoAutomationPackageLibraryProvider() :
                    new AutomationPackageLibraryFromInputStreamProvider(libFileInputStream, libFile.getName());
                AutomationPackageFromInputStreamProvider automationPackageProvider = new AutomationPackageFromInputStreamProvider(automationPackageManager.getAutomationPackageReaderRegistry(),
                    is, apFile.getName(), libFromInputStreamProvider);
                AutomationPackageUpdateParameter localCreateParameters = new AutomationPackageUpdateParameterBuilder().withCreateOnly()
                    .forLocalExecution().build();
                ObjectId automationPackageId = automationPackageManager.createOrUpdateAutomationPackage(
                    automationPackageProvider, libFromInputStreamProvider, localCreateParameters).getId();

                PlanFilter planFilters = getPlanFilters(includePlans, excludePlans, includeCategories, excludeCategories);
                List<StepClassParserResult> listPlans = automationPackageManager.getPackagePlans(automationPackageId)
                    .stream()
                    .filter(planFilters::isSelected)
                    .filter(p -> p.getRoot().getClass().getAnnotation(Artefact.class).validForStandaloneExecution())
                    .map(p -> new StepClassParserResult(getPlanName(p), p, null))
                    .collect(Collectors.toList());

                log.info("The following plans will be executed: {}", quote(listPlans.stream().map(StepClassParserResult::getName)));

                List<String> failedPlans = new ArrayList<>();
                // The live report is only readable if the logs of the engine are not interleaved with it, unless they
                // have been asked for with --verbose or --debug, which the configuration of the local agents reflects
                boolean suppressExecutionLogs = localReports.printsLiveReportToStdout() && !localAgentConfiguration.isVerbose()
                    && !localAgentConfiguration.isDebug();
                try (Restorer ignored = suppressExecutionLogs ? StepConsoleLogging.suppressExecutionLogsUntilClosed() : Restorer.NOTHING) {
                    for (StepClassParserResult parserResult : listPlans) {
                        runPlan(parserResult, executionEngine, executionParameters, localReports, failedPlans);
                    }
                }
                localReports.onAllPlansExecuted(executionEngine.getExecutionEngineContext());
                if (attachmentExporter != null && attachmentExporter.getFileCount() > 0) {
                    log.info("{} attachment(s) written to {}, listed in {}", attachmentExporter.getFileCount(),
                        attachmentExporter.getDirectory().toAbsolutePath().toUri(), AttachmentExporter.INDEX_FILE_NAME);
                }

                // The details of each failure have been logged above. Reporting them here as well is what makes the
                // command exit with an error: a local execution used in a pipeline has to fail the build when a plan
                // fails, exactly like the remote one does.
                if (!failedPlans.isEmpty()) {
                    throw new StepCliExecutionException(failedPlans.size() + "/" + listPlans.size()
                        + " plan(s) failed: " + quote(failedPlans.stream()));
                }
            } catch (FileNotFoundException e) {
                throw new StepCliExecutionException("File not found: " + apFile.getAbsolutePath(), e);
            } catch (IOException e) {
                throw new RuntimeException("IO exception for " + apFile.getAbsolutePath(), e);
            } catch (AutomationPackageReadingException e) {
                throw new RuntimeException("AP reading exception", e);
            } finally {
                if (libFileInputStream != null) {
                    try {
                        libFileInputStream.close();
                    } catch (IOException e) {
                        log.error("Input stream for KW file cannot be closed", e);
                    }
                }
            }
        } catch (IOException e) {
            // Only the file of the live report and the shutdown of the local agents can reach this: the body above
            // handles its own IO errors
            throw new StepCliExecutionException("Error while preparing or stopping the local execution environment", e);
        }
    }

    private static void runPlan(StepClassParserResult parserResult, ExecutionEngine executionEngine, Map<String, String> executionParameters,
                                LocalReports localReports, List<String> failedPlans) {
        new AbstractLocalPlanRunner(parserResult, executionEngine) {
            @Override
            protected void onExecutionStart() {
                log.info("Execution started for plan {}", quote(parserResult.getName()));
            }

            @Override
            protected void onExecutionPassed(PlanRunnerResult result) {
                onPlanExecuted(result);
            }

            @Override
            protected void onExecutionError(PlanRunnerResult result, String errorText, boolean assertionError) {
                onPlanExecuted(result);
                // The status tells the kind of failure, errorText and assertionError being meant for the JUnit runners
                log.error("Execution failed for plan {}. Result status was {}. Errors: {}", quote(parserResult.getName()),
                    result.getResult(), result.getErrorSummary());
                failedPlans.add(parserResult.getName());
            }

            private void onPlanExecuted(PlanRunnerResult result) {
                localReports.onPlanExecuted(parserResult.getName(), result, executionEngine.getExecutionEngineContext());
            }

            @Override
            protected void onInitializingException(Exception exception) {
                log.error("Execution initialization exception for plan {}.", quote(parserResult.getName()), exception);
                failedPlans.add(parserResult.getName());
            }

            @Override
            protected void onExecutionException(Exception exception) {
                log.error("Execution exception for plan {}", quote(parserResult.getName()), exception);
                failedPlans.add(parserResult.getName());
            }

            @Override
            protected void onTestFinished() {
                log.info("Execution finished for plan {}", quote(parserResult.getName()));
            }

            @Override
            protected Map<String, String> getExecutionParameters() {
                return executionParameters;
            }
        }.runPlan();
    }

    /**
     * @return the server, or null if it cannot be started: the execution then runs without it, the agents discarding
     * the files the keywords stream
     */
    private static LocalLiveReportingServer startLiveReportingServer() {
        try {
            return new LocalLiveReportingServer();
        } catch (IOException e) {
            log.warn("Failed to start live reporting server; the files streamed by the keywords will be missing from the reports and the saved attachments", e);
            return null;
        }
    }

    private static ExecutionEngine buildExecutionEngine(LocalAgentProvisioningPlugin localAgents, List<PrintStream> liveReportOutputs,
                                                        AttachmentExporter attachmentExporter,
                                                        LocalLiveReportingServer liveReportingServer) {
        ExecutionEngine.Builder builder = ExecutionEngine.builder()
            .withOperationMode(OperationMode.CLI)
            .withPlugin(localAgents)
            .withPlugin(new AgentProvisioningExecutionPlugin(new AgentProvisioningStatusAccessor(new InMemoryCollection<>())));
        if (liveReportingServer != null) {
            builder.withPlugin(liveReportingServer.getExecutionEnginePlugin());
        }
        // If live reports are used register only LiveReportTreePrinterPlugin which also export attachments, for non-live reports AttachmentExportPlugin is used instead to export attachments directly
        if (!liveReportOutputs.isEmpty()) {
            builder.withPlugin(new LiveReportTreePrinterPlugin(liveReportOutputs, LiveReportTreePrinterPlugin.DEFAULT_STILL_RUNNING_NOTICE_DELAY, attachmentExporter));
        } else if (attachmentExporter != null) {
            builder.withPlugin(new AttachmentExportPlugin(attachmentExporter));
        }
        return builder.withPluginsFromClasspath().build();
    }

    /**
     * @return the plan name in quotes, for the names with spaces to be told apart from the rest of a message
     */
    private static String quote(String planName) {
        return "'" + planName + "'";
    }

    private static String quote(Stream<String> planNames) {
        return planNames.map(ApLocalExecuteCommandHandler::quote).collect(Collectors.joining(", ", "[", "]"));
    }

    private static String getPlanName(Plan p) {
        return p.getAttribute(AbstractOrganizableObject.NAME);
    }
}
