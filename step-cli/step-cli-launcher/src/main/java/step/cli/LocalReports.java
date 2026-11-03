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
import step.cli.reports.ReportRunFolder;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Produces the reports of a local execution chosen with {@code --reportType}, with the same outputs as for a remote
 * execution: the report files are written to the folder of the run and named the same way.
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

    public static final String LIVE_REPORT_FILE_NAME = "live.txt";

    private final List<Report> reports;
    private final ReportRunFolder runFolder;
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
    // The plans executed so far, by the id of their execution
    private final Map<String, String> executionNames = new LinkedHashMap<>();

    /**
     * @param reports   the reports to produce
     * @param runFolder the folder the report files are written to, created when the first of them is written
     */
    public LocalReports(List<Report> reports, ReportRunFolder runFolder) {
        this.reports = List.copyOf(Objects.requireNonNull(reports));
        this.runFolder = Objects.requireNonNull(runFolder);
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
                    File file = new File(runFolder.prepare(), LIVE_REPORT_FILE_NAME);
                    liveReportFile = new PrintStream(new FileOutputStream(file), true, StandardCharsets.UTF_8);
                    outputs.add(liveReportFile);
                    logger.info("The live report is written to {}", ReportFiles.toUri(file));
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
        executionNames.put(result.getExecutionId(), planName);
        List<ReportOutputMode> treeOutputModes = outputModesOf(ReportType.tree);
        if (!treeOutputModes.isEmpty()) {
            ReportFiles.outputTextReport(runFolder, ReportType.tree, result.getExecutionId(), planName,
                getTreeAsString(result, attachmentLines), treeOutputModes, logging);
        }
        List<ReportOutputMode> aggregatedOutputModes = outputModesOf(ReportType.aggregated);
        if (!aggregatedOutputModes.isEmpty()) {
            AggregatedReportView view = new AggregatedReportViewBuilder(engineContext, result.getExecutionId()).buildAggregatedReportView();
            ReportFiles.outputTextReport(runFolder, ReportType.aggregated, result.getExecutionId(), planName,
                String.valueOf(view), aggregatedOutputModes, logging);
        }
    }

    /**
     * Produces the reports of all the plans of the run which have been executed, once they have all been
     */
    public void onAllPlansExecuted(ExecutionEngineContext engineContext) {
        List<ReportOutputMode> junitOutputModes = outputModesOf(ReportType.junit);
        if (!junitOutputModes.isEmpty() && !executionNames.isEmpty()) {
            try {
                JUnitReport report = new JUnitXmlReportBuilder(engineContext)
                    .buildJunitZipReport(new ArrayList<>(executionNames.keySet()), true, JUnitReportCreator.getAttachmentsRootFolder(runFolder));
                JUnitReportCreator.outputZipReport(report.getFileName(), report.getContent(), new ArrayList<>(executionNames.values()), runFolder, junitOutputModes, logging);
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

    private List<ReportOutputMode> outputModesOf(ReportType reportType) {
        return reports.stream()
            .filter(report -> report.getReportType() == reportType)
            .flatMap(report -> report.getOutputModes().stream())
            .distinct()
            .toList();
    }
}
