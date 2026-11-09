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

import ch.exense.commons.io.FileHelper;
import com.google.common.io.Files;
import step.cli.ExecuteAutomationPackageTool;
import step.cli.CliToolLogging;
import step.client.executions.RemoteExecutionManager;
import step.core.execution.model.Execution;
import step.reports.CustomReportType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JUnitReportCreator implements ReportCreator {

    private final RemoteExecutionManager remoteExecutionManager;
    private final ReportRunFolder runFolder;

    public JUnitReportCreator(RemoteExecutionManager remoteExecutionManager, ReportRunFolder runFolder) {
        this.remoteExecutionManager = remoteExecutionManager;
        this.runFolder = runFolder;
    }

    @Override
    public void createReport(Map<String, Execution> executions, List<ExecuteAutomationPackageTool.ReportOutputMode> outputModes, CliToolLogging logging) throws IOException {
        List<String> executionIds = new ArrayList<>(executions.keySet());
        // The links of the report to its attachments start with the folder of the run, sent as their root folder
        RemoteExecutionManager.Report report = executionIds.size() > 1
            ? remoteExecutionManager.getCustomMultiReport(executionIds, CustomReportType.JUNITZIP, true, getAttachmentsRootFolder(runFolder))
            : remoteExecutionManager.getCustomReport(executionIds.get(0), CustomReportType.JUNITZIP, true, getAttachmentsRootFolder(runFolder));
        List<String> executionNames = executions.entrySet().stream()
            .map(execution -> ReportFiles.getExecutionName(execution.getKey(), execution.getValue()))
            .toList();
        outputZipReport(report.getFileName(), report.getContent(), executionNames, runFolder, outputModes, logging);
    }

    /**
     * Outputs a JUnit report prepared as a zip archive, unzipping it to the folder of the run for
     * {@link ExecuteAutomationPackageTool.ReportOutputMode#file}, to a folder named after the archive, and logging its
     * xml file for {@link ExecuteAutomationPackageTool.ReportOutputMode#stdout}
     *
     * @param fileName       the name of the archive, {@code junit.zip}
     * @param executionNames the names of the executions the report is about, typically their plans
     */
    public static void outputZipReport(String fileName, byte[] zip, List<String> executionNames, ReportRunFolder runFolder,
                                       List<ExecuteAutomationPackageTool.ReportOutputMode> outputModes,
                                       CliToolLogging logging) throws IOException {
        for (ExecuteAutomationPackageTool.ReportOutputMode outputMode : outputModes) {
            switch (outputMode) {
                case file:
                    File folder = new File(runFolder.prepare(), Files.getNameWithoutExtension(fileName));
                    FileHelper.unzip(zip, folder);
                    logging.logInfo("The " + ExecuteAutomationPackageTool.ReportType.junit + " report has been saved in "
                        + ReportFiles.toUri(folder), null);
                    break;
                case stdout:
                    logging.logInfo(ReportFiles.title(ExecuteAutomationPackageTool.ReportType.junit, executionNames) + "\n"
                        + readXmlReport(zip), null);
                    break;
                default:
                    logging.logError("Unsupported output mode (" + outputMode + " ) for junit report", null);
            }
        }
    }

    /**
     * @return the xml file at the root of the archive, the other files being its attachments
     */
    private static String readXmlReport(byte[] zip) throws IOException {
        try (ZipInputStream entries = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entry = entries.getNextEntry(); entry != null; entry = entries.getNextEntry()) {
                // Named junit.xml, or <timestamp>-junit.xml by the controllers of the versions before
                if (!entry.isDirectory() && !entry.getName().contains("/") && entry.getName().endsWith("junit.xml")) {
                    return new String(entries.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IOException("The JUnit report archive contains no xml report");
    }

    /**
     * @return the path of the attachment root folder, relative to the working directory when possible, falling back to absolute path otherwise
     */
    public static String getAttachmentsRootFolder(ReportRunFolder runFolder) {
        return getAttachmentsRootFolder(new File("").toPath().toAbsolutePath(), runFolder.getFolder().toPath().toAbsolutePath());
    }

    static String getAttachmentsRootFolder(Path currentFolder, Path runFolder) {
        try {
            return currentFolder.relativize(runFolder).toString();
        } catch (IllegalArgumentException e) {
            // The paths have different roots
            return runFolder.toString();
        }
    }
}
