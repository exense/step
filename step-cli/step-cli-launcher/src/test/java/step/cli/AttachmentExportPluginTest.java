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
package step.cli;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.TestCase;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.engine.plugins.FunctionPlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.threadpool.ThreadPoolPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static step.planbuilder.BaseArtefacts.childrenBlock;
import static step.planbuilder.BaseArtefacts.sequence;
import static step.planbuilder.BaseArtefacts.testCase;
import static step.planbuilder.FunctionArtefacts.keyword;

/**
 * Covers the attachments written when the report is not printed live, by the {@link AttachmentExportPlugin}, using
 * the keywords of {@link LiveReportTreePrinterPluginTest.OnlineShopKeywords}
 */
public class AttachmentExportPluginTest {

    @Rule
    public final TemporaryFolder reportDirectory = new TemporaryFolder();

    /**
     * Two plans of a single run of the CLI, whose names only differ by characters a folder name cannot hold
     */
    @Test
    public void writesTheAttachmentsOfEveryPlanOfTheRunToAFolderOfItsOwn() throws IOException {
        AttachmentExporter exporter = new AttachmentExporter(reportDirectory.getRoot().toPath());
        try (ExecutionEngine executionEngine = ExecutionEngine.builder()
            .withPlugin(new AttachmentExportPlugin(exporter))
            .withPlugin(new ThreadPoolPlugin())
            .withPlugin(new BaseArtefactPlugin())
            .withPlugin(new FunctionPlugin())
            .withPlugin(new LocalFunctionPlugin())
            .build()) {
            executionEngine.execute(screenshotPlan("Shop: checkout"));
            executionEngine.execute(screenshotPlan("Shop/checkout"));
        }

        Path runDirectory = exporter.getRunDirectory();
        Assert.assertEquals(6, exporter.getFileCount());
        Assert.assertTrue(Files.isRegularFile(runDirectory.resolve("Shop_checkout").resolve("0001-Take_screenshot").resolve("page-2.png")));
        Assert.assertTrue(Files.isRegularFile(runDirectory.resolve("Shop_checkout-2").resolve("0002-Take_screenshot").resolve("page-2.png")));

        List<String> pathsAndFiles = Files.readAllLines(runDirectory.resolve(AttachmentExporter.INDEX_FILE_NAME)).stream()
            .skip(1)
            .map(line -> line.split("\t"))
            .map(fields -> fields[0] + " | " + fields[1] + " | " + fields[5])
            .distinct()
            .collect(Collectors.toList());
        Assert.assertEquals(List.of(
            "Shop: checkout | Checkout > [BEFORE] > Take screenshot | Shop_checkout/0001-Take_screenshot/page.png",
            "Shop: checkout | Checkout > [BEFORE] > Take screenshot | Shop_checkout/0001-Take_screenshot/page-2.png",
            "Shop: checkout | Checkout > [BEFORE] > Take screenshot | Shop_checkout/0001-Take_screenshot/console__12_11_.log",
            "Shop/checkout | Checkout > [BEFORE] > Take screenshot | Shop_checkout-2/0002-Take_screenshot/page.png",
            "Shop/checkout | Checkout > [BEFORE] > Take screenshot | Shop_checkout-2/0002-Take_screenshot/page-2.png",
            "Shop/checkout | Checkout > [BEFORE] > Take screenshot | Shop_checkout-2/0002-Take_screenshot/console__12_11_.log"
        ), pathsAndFiles);
    }

    @Test
    public void namesTheFilesAndFoldersWithCharactersEveryFileSystemAccepts() {
        Assert.assertEquals("Order_an_article", AttachmentExporter.toFolderName("Order an article"));
        Assert.assertEquals("Echo", AttachmentExporter.toFolderName("Echo..."));
        Assert.assertEquals("_", AttachmentExporter.toFolderName(null));
        Assert.assertEquals(50, AttachmentExporter.toFolderName("x".repeat(80)).length());

        Assert.assertEquals("screenshot_1.png", AttachmentExporter.toFileName(" screenshot 1.png "));
        Assert.assertEquals("a_b_c_d.txt", AttachmentExporter.toFileName("a/b\\c:d.txt"));
        Assert.assertEquals("attachment", AttachmentExporter.toFileName(""));
        Assert.assertEquals("attachment", AttachmentExporter.toFileName(".."));
    }

    private static Plan screenshotPlan(String planName) {
        CallFunction takeScreenshot = keyword("Take screenshot");
        takeScreenshot.getAttributes().put(AbstractOrganizableObject.NAME, "Take screenshot");
        TestCase testCase = testCase("Checkout");
        testCase.setBefore(childrenBlock(takeScreenshot));
        Plan plan = PlanBuilder.create()
            .startBlock(testCase)
            .add(sequence())
            .endBlock().build();
        plan.addAttribute(AbstractOrganizableObject.NAME, planName);
        return plan;
    }
}
