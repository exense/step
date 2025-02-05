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

import step.cli.AbstractExecuteAutomationPackageTool;
import step.cli.CliToolLogging;
import step.cli.StepCliExecutionException;
import step.client.executions.RemoteExecutionManager;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.execution.model.Execution;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class AggregatedReportCreator implements ReportCreator {

    private final RemoteExecutionManager remoteExecutionManager;
    private final File outputFolder;

    public AggregatedReportCreator(RemoteExecutionManager remoteExecutionManager, File outputFolder) {
        this.remoteExecutionManager = remoteExecutionManager;
        this.outputFolder = outputFolder;
    }

    @Override
    public void createReport(Map<String, Execution> executions, List<AbstractExecuteAutomationPackageTool.ReportOutputMode> outputModes, CliToolLogging logging) {
        for (String executionId : executions.keySet()) {
            AggregatedReportView aggregatedReportView = remoteExecutionManager.getAggregatedReportView(executionId);

            for (AbstractExecuteAutomationPackageTool.ReportOutputMode outputMode : outputModes) {
                switch (outputMode) {
                    case stdout:
                        logging.logInfo("Aggregated report:\n" + aggregatedReportView.toString(), null);
                        break;
                    case file:
                        Execution executionInfo = executions.get(executionId);
                        String executionName;
                        if (executionInfo == null || executionInfo.getDescription() == null) {
                            executionName = executionId;
                        } else {
                            executionName = sanitizeFileName(executionInfo.getDescription());
                        }
                        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSSSSS").format(LocalDateTime.now());
                        String fileName = String.format("%s-%s-aggregated.txt", executionName, timestamp);
                        File reportFile = new File(outputFolder, fileName);
                        try (FileOutputStream fos = new FileOutputStream(reportFile)) {
                            fos.write(aggregatedReportView.toString().getBytes());
                        } catch (IOException e) {
                            throw new StepCliExecutionException("Unable to prepare the aggregated report file", e);
                        }
                        logging.logInfo("The aggregated report has been saved in " + reportFile.getAbsolutePath(), null);
                        break;
                    default:
                        logging.logError("Unsupported output mode (" + outputMode + " ) for aggregated report report", null);
                }
            }
        }
    }

    /**
     * Sanitizes the file name by replacing all special characters with '_'
     */
    protected String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9\\._]+", "_");
    }
}
