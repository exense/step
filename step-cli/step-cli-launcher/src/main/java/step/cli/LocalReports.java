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
import step.cli.ExecuteAutomationPackageTool.Report;
import step.cli.ExecuteAutomationPackageTool.ReportOutputMode;
import step.cli.ExecuteAutomationPackageTool.ReportType;
import step.cli.reports.JUnitReportCreator;
import step.cli.reports.ReportFiles;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.artefacts.reports.aggregated.AggregatedReportViewBuilder;
import step.core.artefacts.reports.junitxml.JUnitXmlReportBuilder;
import step.core.execution.ExecutionEngineContext;
import step.core.plans.runner.PlanRunnerResult;
import step.reporting.JUnitReport;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Produces the reports of a local execution chosen with {@code --reportType}, with the same outputs as for a remote
 * execution: the report files are written to the report directory and named the same way.
 * <ul>
 *     <li>live is printed while the plans execute, to a single file for all of them</li>
 *     <li>tree and aggregated are produced once each plan has been executed</li>
 *     <li>junit is produced once all the plans have been executed, for all of them, with their attachments</li>
 * </ul>
 * Every report is built before the execution engine is closed, which deletes the attachments.
 */
public class LocalReports implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(LocalReports.class);

    /**
     * The reports of a local execution for which none has been chosen
     */
    public static final List<Report> DEFAULT_REPORTS = List.of(new Report(ReportType.tree));

    private final List<Report> reports;
    private final File reportDir;
    private final CliToolLogging logging = new CliToolLogging() {
        @Override
        public void logError(String errorText, Throwable e) {
            logger.error(errorText, e);
        }

        @Override
        public void logInfo(String infoText, Throwable e) {
            logger.info(infoText, e);
        }

        @Override
        public void logDebug(String infoText, Throwable e) {
            logger.debug(infoText, e);
        }
    };
    private PrintStream liveReportFile;

    /**
     * @param reports   the reports to produce
     * @param reportDir the directory the report files are written to, created when the first of them is written
     */
    public LocalReports(List<Report> reports, File reportDir) {
        this.reports = List.copyOf(Objects.requireNonNull(reports));
        this.reportDir = Objects.requireNonNull(reportDir);
    }

    /**
     * @return if the live report is printed to the standard output, which the logs of the execution would make
     * unreadable
     */
    public boolean printsLiveReportToStdout() {
        return outputModesOf(ReportType.live).contains(ReportOutputMode.stdout);
    }

    /**
     * @return the outputs the live report is printed to, none if it has not been chosen
     */
    public List<PrintStream> openLiveReportOutputs() throws IOException {
        List<PrintStream> outputs = new ArrayList<>();
        for (ReportOutputMode outputMode : outputModesOf(ReportType.live)) {
            switch (outputMode) {
                case stdout:
                    outputs.add(System.out);
                    break;
                case file:
                    File file = new File(ReportFiles.prepareOutputFolder(reportDir), ReportFiles.timestamp() + "-live.txt");
                    liveReportFile = new PrintStream(new FileOutputStream(file), true, StandardCharsets.UTF_8);
                    outputs.add(liveReportFile);
                    logger.info("The live report is written to {}", file.toPath().toAbsolutePath().toUri());
                    break;
                default:
                    logger.error("Unsupported output mode ({}) for live report", outputMode);
            }
        }
        return outputs;
    }

    /**
     * Produces the reports of a plan which has been executed
     *
     * @param attachmentLines the lines printed below each node of the report tree, telling where its attachments have
     *                        been written
     */
    public void onPlanExecuted(String planName, PlanRunnerResult result, ExecutionEngineContext engineContext,
                               Function<ReportNode, List<String>> attachmentLines) {
        List<ReportOutputMode> treeOutputModes = outputModesOf(ReportType.tree);
        if (!treeOutputModes.isEmpty()) {
            ReportFiles.outputTextReport(reportDir(treeOutputModes), ReportType.tree, planName,
                getTreeAsString(result, attachmentLines), treeOutputModes, logging);
        }
        List<ReportOutputMode> aggregatedOutputModes = outputModesOf(ReportType.aggregated);
        if (!aggregatedOutputModes.isEmpty()) {
            AggregatedReportView view = new AggregatedReportViewBuilder(engineContext, result.getExecutionId()).buildAggregatedReportView();
            ReportFiles.outputTextReport(reportDir(aggregatedOutputModes), ReportType.aggregated, planName,
                String.valueOf(view), aggregatedOutputModes, logging);
        }
    }

    /**
     * Produces the reports of all the plans of the run, once they have been executed
     */
    public void onAllPlansExecuted(List<String> executionIds, ExecutionEngineContext engineContext) {
        List<ReportOutputMode> junitOutputModes = outputModesOf(ReportType.junit);
        if (!junitOutputModes.isEmpty() && !executionIds.isEmpty()) {
            try {
                // Prepared in the report directory, as the remote ones are, whatever the output
                File outputFolder = ReportFiles.prepareOutputFolder(reportDir);
                JUnitReport report = new JUnitXmlReportBuilder(engineContext)
                    .buildJunitZipReport(executionIds, true, JUnitReportCreator.getAttachmentsRootFolder(outputFolder));
                File preparedReport = new File(outputFolder, report.getFileName());
                Files.write(preparedReport.toPath(), report.getContent());
                JUnitReportCreator.outputZipReport(preparedReport, outputFolder, junitOutputModes, logging);
            } catch (IOException e) {
                logger.error("The {} report cannot be saved", ReportType.junit, e);
            }
        }
    }

    @Override
    public void close() {
        if (liveReportFile != null) {
            liveReportFile.close();
        }
    }

    /**
     * The tree of the execution with the details of the nodes, the stack traces of the technical errors, and where the
     * attachments of the nodes have been written
     */
    static String getTreeAsString(PlanRunnerResult result, Function<ReportNode, List<String>> attachmentLines) {
        StringWriter writer = new StringWriter();
        try {
            result.printTree(writer, true, true, attachmentLines);
            return writer.toString();
        } catch (IOException e) {
            logger.error("Error while writing execution tree: {}", writer, e);
            return "Error while writing tree. See logs for details.";
        }
    }

    private File reportDir(List<ReportOutputMode> outputModes) {
        return outputModes.contains(ReportOutputMode.file) ? ReportFiles.prepareOutputFolder(reportDir) : reportDir;
    }

    private List<ReportOutputMode> outputModesOf(ReportType reportType) {
        return reports.stream()
            .filter(report -> report.getReportType() == reportType)
            .flatMap(report -> report.getOutputModes().stream())
            .distinct()
            .toList();
    }
}
