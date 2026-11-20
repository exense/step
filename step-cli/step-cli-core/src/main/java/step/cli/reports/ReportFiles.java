/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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
import java.util.List;

/**
 * Helper used to print the reports to stdout and writes them to the report folder of the run. Used for both remote and local executions.
 */
public final class ReportFiles {

    private ReportFiles() {
    }

    /**
     * Writes a text report of an execution to {@code <execution name>-<report type>.txt}
     */
    private static void writeTextReport(ReportRunFolder runFolder, String executionId, String executionName,
                                        ReportType reportType, String content, CliToolLogging logging) {
        File reportFile = new File(runFolder.prepare(),
            runFolder.getExecutionFileName(executionId, executionName) + "-" + reportType.name() + ".txt");
        try {
            Files.writeString(reportFile.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StepCliExecutionException("Unable to write the " + reportType + " report file", e);
        }
        logging.logInfo("The " + reportType + " report has been saved in " + toUri(reportFile), null);
    }

    /**
     * Outputs a text report of an execution, logging it for {@link ReportOutputMode#stdout} and writing it to the
     * folder of the run for {@link ReportOutputMode#file}
     *
     * @param executionName the name of the execution, typically its plan
     */
    public static void outputTextReport(ReportRunFolder runFolder, ReportType reportType, String executionId,
                                        String executionName, String report, List<ReportOutputMode> outputModes,
                                        CliToolLogging logging) {
        for (ReportOutputMode outputMode : outputModes) {
            switch (outputMode) {
                case stdout:
                    logging.logInfo(title(reportType, List.of(executionName)) + "\n" + report, null);
                    break;
                case file:
                    writeTextReport(runFolder, executionId, executionName, reportType, report, logging);
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

    /**
     * @param executionNames the names of the executions the report is about, typically their plans
     * @return the line printed before a report, such as {@code Tree report of My plan:}
     */
    public static String title(ReportType reportType, List<String> executionNames) {
        return capitalize(reportType.name()) + " report of " + String.join(", ", executionNames) + ":";
    }

    /**
     * @return the absolute file URI of the file, which terminals and IDE consoles turn into a link
     */
    public static String toUri(File file) {
        return file.toPath().toAbsolutePath().toUri().toString();
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
