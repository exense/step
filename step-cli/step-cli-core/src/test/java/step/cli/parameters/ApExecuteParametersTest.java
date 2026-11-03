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
package step.cli.parameters;

import org.junit.Assert;
import org.junit.Test;
import step.cli.ExecuteAutomationPackageTool.Report;
import step.cli.ExecuteAutomationPackageTool.ReportType;
import step.cli.StepCliExecutionException;

import java.util.List;

public class ApExecuteParametersTest {

    /**
     * The report nodes of a remote execution are only fetched once it has ended, by the CLI and the Maven plugin alike
     */
    @Test
    public void rejectsTheLiveReportOfARemoteExecution() {
        new ApExecuteParameters().setReports(List.of(new Report(ReportType.tree), new Report(ReportType.junit))).validate();

        StepCliExecutionException exception = Assert.assertThrows(StepCliExecutionException.class,
            () -> new ApExecuteParameters().setReports(List.of(new Report(ReportType.live))).validate());
        Assert.assertEquals("The live report is only supported for local executions", exception.getMessage());
    }
}
