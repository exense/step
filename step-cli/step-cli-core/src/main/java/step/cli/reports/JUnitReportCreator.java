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
import step.cli.AbstractExecuteAutomationPackageTool;
import step.cli.CliToolLogging;
import step.cli.StepCliExecutionException;
import step.client.executions.RemoteExecutionManager;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.reports.CustomReportType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class JUnitReportCreator implements ReportCreator {

    private final RemoteExecutionManager remoteExecutionManager;
    private final File outputFolder;

    public JUnitReportCreator(RemoteExecutionManager remoteExecutionManager, File outputFolder) {
        this.remoteExecutionManager = remoteExecutionManager;
        this.outputFolder = outputFolder;
    }

    @Override
    public void createReport(List<String> executionIds, List<AbstractExecuteAutomationPackageTool.ReportOutputMode> outputModes, CliToolLogging logging) throws IOException {
        File preparedReport = prepareCustomReportOnServer(executionIds, logging);

        for (AbstractExecuteAutomationPackageTool.ReportOutputMode outputMode : outputModes) {
            switch (outputMode) {
                case filesystem:
                    // automatically unzip file
                    File folderToUnzip = new File(outputFolder, Files.getNameWithoutExtension(preparedReport.getName()));
                    if (!folderToUnzip.exists()) {
                        folderToUnzip.mkdir();
                    }
                    logging.logInfo("Unzip the report into " + folderToUnzip, null);
                    FileHelper.unzip(preparedReport, folderToUnzip);
                    if (!preparedReport.delete()) {
                        logging.logInfo("File cannot be deleted: " + preparedReport.getAbsolutePath(), null);
                    }
                    break;
                case stdout:
                    // create temp folder with unzipped report to read and print the content of the main .xml file
                    File tempFolderToUnzip = new File(outputFolder, DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSSSS").format(LocalDateTime.now()));
                    tempFolderToUnzip.mkdir();
                    FileHelper.unzip(preparedReport, tempFolderToUnzip);

                    // according to the structure of zipped report, the main junit report is called like {timestamp}-junit.xml
                    File[] xmlReports = tempFolderToUnzip.listFiles((dir, name) -> name.matches(".*-junit.xml"));
                    if(xmlReports != null) {
                        for (File xmlReport : xmlReports) {
                            logging.logInfo("Junit report:\n" + new String(java.nio.file.Files.readAllBytes(xmlReport.toPath())), null);
                        }
                    }

                    FileHelper.deleteFolder(tempFolderToUnzip);
                    if (!preparedReport.delete()) {
                        logging.logInfo("File cannot be deleted: " + preparedReport.getAbsolutePath(), null);
                    }
                    break;
                default:
                    logging.logError("Unsupported output mode (" + outputMode + " ) for junit report", null);
            }
        }
    }

    protected File prepareCustomReportOnServer(List<String> executionIds, CliToolLogging logging) throws IOException {
        // only for zipped reports we want to include attachments
        Boolean includeAttachments = true;

        RemoteExecutionManager.Report customReport;
        File outputFile;

        Path currentFolder = new File("").toPath().toAbsolutePath();
        Path relativePathToOutputDir = currentFolder.relativize(outputFolder.toPath().toAbsolutePath());

        // report output directory is sent as a root folder for attachments
        if (executionIds.size() > 1) {
            customReport = remoteExecutionManager.getCustomMultiReport(executionIds, CustomReportType.JUNITZIP, includeAttachments, relativePathToOutputDir.toFile().getPath());
            outputFile = new File(outputFolder, customReport.getFileName());
        } else {
            customReport = remoteExecutionManager.getCustomReport(executionIds.get(0), CustomReportType.JUNITZIP, includeAttachments, relativePathToOutputDir.toFile().getPath());
            outputFile = new File(outputFolder, customReport.getFileName());
        }
        logging.logInfo("Saving execution report (" + AbstractExecuteAutomationPackageTool.ReportType.junit + ") into " + outputFile.getAbsolutePath(), null);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(customReport.getContent());
        }
        return outputFile;
    }
}
