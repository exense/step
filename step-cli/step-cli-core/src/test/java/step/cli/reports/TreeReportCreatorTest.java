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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import step.cli.CliToolLogging;
import step.cli.ExecuteAutomationPackageTool.ReportOutputMode;
import step.client.executions.RemoteExecutionFuture;
import step.client.executions.RemoteExecutionManager;
import step.core.execution.model.Execution;

import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TreeReportCreatorTest {

    @Rule
    public final TemporaryFolder reportFolder = new TemporaryFolder();

    @Test
    public void outputsTheTreeOfEachRemoteExecution() throws Exception {
        RemoteExecutionFuture future = Mockito.mock(RemoteExecutionFuture.class);
        Mockito.when(future.printTree(Mockito.any(Writer.class), Mockito.eq(true), Mockito.eq(false))).thenAnswer(invocation -> {
            invocation.getArgument(0, Writer.class).write("Echo(Hello):PASSED:\n");
            return future;
        });
        RemoteExecutionManager executionManager = Mockito.mock(RemoteExecutionManager.class);
        Mockito.when(executionManager.getFuture("execution-1")).thenReturn(future);
        Execution execution = Mockito.mock(Execution.class);
        Mockito.when(execution.getDescription()).thenReturn("My plan");
        List<String> logged = new ArrayList<>();

        new TreeReportCreator(executionManager, new ReportRunFolder(reportFolder.getRoot(), LocalDateTime.of(2026, 11, 3, 10, 15, 30)))
            .createReport(Map.of("execution-1", execution), List.of(ReportOutputMode.stdout, ReportOutputMode.file), logging(logged));

        Assert.assertEquals("Tree report of My plan:\nEcho(Hello):PASSED:\n", logged.get(0));
        File file = reportFolder.getRoot().toPath().resolve("20261103-101530-000000").resolve("My_plan-tree.txt").toFile();
        Assert.assertEquals("Echo(Hello):PASSED:\n", Files.readString(file.toPath()));
    }

    private static CliToolLogging logging(List<String> logged) {
        return new CliToolLogging() {
            @Override
            public void logError(String errorText, Throwable e) {
                logged.add(errorText);
            }

            @Override
            public void logInfo(String infoText, Throwable e) {
                logged.add(infoText);
            }

            @Override
            public void logDebug(String infoText, Throwable e) {
            }
        };
    }
}
