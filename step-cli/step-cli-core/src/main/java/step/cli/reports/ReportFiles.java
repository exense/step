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
package step.cli.reports;

import step.cli.CliToolLogging;
import step.cli.ExecuteAutomationPackageTool.ReportOutputMode;
import step.cli.ExecuteAutomationPackageTool.ReportType;
import step.cli.StepCliExecutionException;
import step.core.execution.model.Execution;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Prints the reports to stdout and/or writes them to the report directory. Used for both remote and local executions.
 */
public final class ReportFiles {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSSSS");

    private ReportFiles() {
    }

    /**
     * @return the timestamp the names of the report files and folders start or end with
     */
    public static String timestamp() {
        return TIMESTAMP.format(LocalDateTime.now());
    }

    /**
     * Sanitizes the file name by replacing all special characters with '_'
     */
    public static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9\\._]+", "_");
    }

    /**
     * Writes a text report of an execution to {@code <execution name>-<timestamp>-<report type>.txt}
     *
     * @param executionName the name of the execution, typically its plan, or its id when it has none
     */
    private static File writeTextReport(File outputFolder, String executionName, String reportType, String content,
                                        CliToolLogging logging) {
        File reportFile = new File(outputFolder, String.format("%s-%s-%s.txt", sanitizeFileName(executionName), timestamp(), reportType));
        try {
            Files.writeString(reportFile.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StepCliExecutionException("Unable to prepare the " + reportType + " report file", e);
        }
        // As a file URI, which terminals and IDE consoles turn into a link
        logging.logInfo("The " + reportType + " report has been saved in " + reportFile.toPath().toAbsolutePath().toUri(), null);
        return reportFile;
    }

    /**
     * Outputs a text report of an execution, logging it for {@link ReportOutputMode#stdout} and writing it to the
     * report directory for {@link ReportOutputMode#file}
     */
    public static void outputTextReport(File outputFolder, ReportType reportType, String executionName, String report,
                                        List<ReportOutputMode> outputModes, CliToolLogging logging) {
        for (ReportOutputMode outputMode : outputModes) {
            switch (outputMode) {
                case stdout:
                    logging.logInfo(capitalize(reportType.name()) + " report:\n" + report, null);
                    break;
                case file:
                    writeTextReport(outputFolder, executionName, reportType.name(), report, logging);
                    break;
                default:
                    logging.logError("Unsupported output mode (" + outputMode + " ) for " + reportType + " report", null);
            }
        }
    }

    /**
     * @return the description of the execution, which is the name of its plan for the executions started one per
     * plan, or its id when it has none
     */
    public static String getExecutionName(String executionId, Execution executionInfo) {
        return executionInfo == null || executionInfo.getDescription() == null ? executionId : executionInfo.getDescription();
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /**
     * Creates the report directory if it does not exist yet
     */
    public static File prepareOutputFolder(File outputFolder) {
        if (outputFolder.exists()) {
            if (!outputFolder.isDirectory()) {
                throw new StepCliExecutionException("Report cannot be generated. Invalid folder: " + outputFolder.getAbsolutePath());
            }
        } else if (!outputFolder.mkdirs()) {
            throw new StepCliExecutionException("Report cannot be generated. Folder hasn't been created: " + outputFolder.getAbsolutePath());
        }
        return outputFolder;
    }
}
