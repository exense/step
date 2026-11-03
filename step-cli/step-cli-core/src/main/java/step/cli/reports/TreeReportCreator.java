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
import step.cli.ExecuteAutomationPackageTool;
import step.client.executions.RemoteExecutionManager;
import step.core.execution.model.Execution;
import step.core.plans.runner.PlanRunnerResult;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * The report tree of remote executions, as printed by the local ones.
 * <p>
 * The tree is fetched from the controller one node after another, as the visit of the tree requires them, for a large
 * execution to put no more load on the controller at a time than a single node does.
 */
public class TreeReportCreator implements ReportCreator {

    private final RemoteExecutionManager remoteExecutionManager;
    private final ReportRunFolder runFolder;

    public TreeReportCreator(RemoteExecutionManager remoteExecutionManager, ReportRunFolder runFolder) {
        this.remoteExecutionManager = remoteExecutionManager;
        this.runFolder = runFolder;
    }

    @Override
    public void createReport(Map<String, Execution> executions, List<ExecuteAutomationPackageTool.ReportOutputMode> outputModes, CliToolLogging logging) throws IOException {
        for (String executionId : executions.keySet()) {
            String report = getTreeAsString(remoteExecutionManager.getFuture(executionId));
            ReportFiles.outputTextReport(runFolder, ExecuteAutomationPackageTool.ReportType.tree, executionId,
                ReportFiles.getExecutionName(executionId, executions.get(executionId)), report, outputModes, logging);
        }
    }

    protected String getTreeAsString(PlanRunnerResult result) throws IOException {
        StringWriter writer = new StringWriter();
        // Without the content of the attachments, which are stored on the controller
        result.printTree(writer, true, false);
        return writer.toString();
    }
}
