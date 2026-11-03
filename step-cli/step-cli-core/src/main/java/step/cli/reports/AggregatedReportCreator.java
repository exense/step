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

import step.cli.ExecuteAutomationPackageTool;
import step.cli.CliToolLogging;
import step.client.executions.RemoteExecutionManager;
import step.core.execution.model.Execution;

import java.io.File;
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
    public void createReport(Map<String, Execution> executions, List<ExecuteAutomationPackageTool.ReportOutputMode> outputModes, CliToolLogging logging) {
        for (String executionId : executions.keySet()) {
            String report = remoteExecutionManager.getAggregatedReportView(executionId).toString();
            ReportFiles.outputTextReport(outputFolder, ExecuteAutomationPackageTool.ReportType.aggregated,
                ReportFiles.getExecutionName(executionId, executions.get(executionId)), report, outputModes, logging);
        }
    }
}
