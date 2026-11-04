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
package step.cli;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.cli.ExecuteAutomationPackageTool.Report;
import step.cli.ExecuteAutomationPackageTool.ReportOutputMode;
import step.cli.ExecuteAutomationPackageTool.ReportType;
import step.cli.reports.ReportRunFolder;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.FunctionPlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.threadpool.ThreadPoolPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static step.planbuilder.BaseArtefacts.testCase;
import static step.planbuilder.FunctionArtefacts.keywordWithDynamicKeyValues;

/**
 * Covers the tree report of a local execution, using the keywords of
 * {@link LiveReportTreePrinterPluginTest.OnlineShopKeywords}
 */
public class LocalReportsTest {

    private static final List<Report> TREE_REPORT = List.of(new Report(ReportType.tree, List.of(ReportOutputMode.file)));

    @Rule
    public final TemporaryFolder reportDir = new TemporaryFolder();

    @Test
    public void printsTheStackTraceOfATechnicalErrorInTheTree() throws IOException {
        ReportRunFolder runFolder = runFolder();

        String tree = executeAndPrintTree(runFolder, null);

        Assert.assertTrue(tree, tree.contains("Place order(Input={\"token\":\"expired\"}, Output={}):TECHNICAL_ERROR:The token 'expired' is no longer valid"));
        Assert.assertTrue(tree, tree.contains("Stacktrace: \njava.lang.IllegalStateException: The token 'expired' is no longer valid"));
    }

    /**
     * The stack trace being in the exception.log the tree links to
     */
    @Test
    public void linksTheSavedExceptionLogOfATechnicalErrorRatherThanPrintingItsStackTrace() throws IOException {
        ReportRunFolder runFolder = runFolder();
        AttachmentExporter attachmentExporter = new AttachmentExporter(runFolder);

        String tree = executeAndPrintTree(runFolder, attachmentExporter);

        Path exceptionLog = attachmentExporter.getDirectory().resolve("Online_shop").resolve("0001-Place_order").resolve("exception.log");
        Assert.assertTrue(tree, tree.contains("Place order(Input={\"token\":\"expired\"}, Output={}):TECHNICAL_ERROR:The token 'expired' is no longer valid\n"
            + "   attachment: " + exceptionLog.toAbsolutePath().toUri()));
        Assert.assertFalse(tree, tree.contains("Stacktrace"));
    }

    private ReportRunFolder runFolder() {
        return new ReportRunFolder(reportDir.getRoot(), LocalDateTime.of(2026, 11, 4, 10, 0));
    }

    private static String executeAndPrintTree(ReportRunFolder runFolder, AttachmentExporter attachmentExporter) throws IOException {
        CallFunction placeOrder = keywordWithDynamicKeyValues("Place order", false, "token", "'expired'");
        placeOrder.getAttributes().put(AbstractOrganizableObject.NAME, "Place order");
        Plan plan = PlanBuilder.create()
            .startBlock(testCase("Order with an expired token"))
            .add(placeOrder)
            .endBlock().build();
        plan.addAttribute(AbstractOrganizableObject.NAME, "Online shop");

        ExecutionEngine.Builder builder = ExecutionEngine.builder()
            .withPlugin(new ThreadPoolPlugin())
            .withPlugin(new BaseArtefactPlugin())
            .withPlugin(new FunctionPlugin())
            .withPlugin(new LocalFunctionPlugin());
        if (attachmentExporter != null) {
            builder.withPlugin(new AttachmentExportPlugin(attachmentExporter));
        }
        try (ExecutionEngine executionEngine = builder.build();
             LocalReports localReports = new LocalReports(TREE_REPORT, runFolder, attachmentExporter)) {
            PlanRunnerResult result = executionEngine.execute(plan);
            localReports.onPlanExecuted("Online shop", result, executionEngine.getExecutionEngineContext());
        }
        return Files.readString(runFolder.getFolder().toPath().resolve("Online_shop-tree.txt"));
    }
}
