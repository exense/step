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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.time.LocalDateTime;

public class ReportRunFolderTest {

    @Rule
    public final TemporaryFolder reportDir = new TemporaryFolder();

    @Test
    public void isNamedAfterTheStartOfTheRunAndOnlyCreatedWhenPrepared() {
        File parent = new File(reportDir.getRoot(), "reports");
        ReportRunFolder runFolder = new ReportRunFolder(parent, LocalDateTime.of(2026, 11, 3, 10, 15, 30, 123456000));

        Assert.assertEquals(new File(parent, "20261103-101530-123456"), runFolder.getFolder());
        Assert.assertFalse(parent.exists());
        Assert.assertTrue(runFolder.prepare().isDirectory());
    }

    /**
     * Executions whose names only differ by the characters a file name cannot hold, or by their case
     */
    @Test
    public void namesEveryExecutionTheSameWayAndApartFromTheOthers() {
        ReportRunFolder runFolder = new ReportRunFolder(reportDir.getRoot());

        Assert.assertEquals("Shop_checkout", runFolder.getExecutionFileName("1", "Shop: checkout"));
        Assert.assertEquals("Shop_checkout-2", runFolder.getExecutionFileName("2", "Shop/checkout"));
        Assert.assertEquals("shop_checkout-3", runFolder.getExecutionFileName("3", "shop checkout"));
        Assert.assertEquals("Shop_checkout", runFolder.getExecutionFileName("1", "Shop: checkout"));
        Assert.assertEquals("4", runFolder.getExecutionFileName("4", null));
    }

    @Test
    public void namesTheFilesWithCharactersEveryFileSystemAccepts() {
        Assert.assertEquals("Order_an_article", ReportRunFolder.toFileName("Order an article", "_"));
        Assert.assertEquals("screenshot_1.png", ReportRunFolder.toFileName(" screenshot 1.png ", "_"));
        Assert.assertEquals("a_b_c_d.txt", ReportRunFolder.toFileName("a/b\\c:d.txt", "_"));
        Assert.assertEquals("console_12_11_.log", ReportRunFolder.toFileName("console: 12/11?.log", "_"));
        Assert.assertEquals("Récupération_données", ReportRunFolder.toFileName("Récupération données", "_"));
        Assert.assertEquals("Echo", ReportRunFolder.toFileName("Echo...", "_"));
    }

    @Test
    public void namesTheFilesWithNothingLeftAfterTheFallback() {
        Assert.assertEquals("_", ReportRunFolder.toFileName(null, "_"));
        Assert.assertEquals("attachment", ReportRunFolder.toFileName("", "attachment"));
        Assert.assertEquals("attachment", ReportRunFolder.toFileName("..", "attachment"));
    }

    @Test
    public void truncatesTheNamesWithoutLosingTheirExtension() {
        Assert.assertEquals("x".repeat(50), ReportRunFolder.toFileName("x".repeat(80), "_"));
        Assert.assertEquals("x".repeat(46) + ".png", ReportRunFolder.toFileName("x".repeat(80) + ".png", "_"));
        // Too long to be an extension
        Assert.assertEquals("x." + "y".repeat(48), ReportRunFolder.toFileName("x." + "y".repeat(80), "_"));
    }

    @Test
    public void prefixesTheNamesWindowsReserves() {
        Assert.assertEquals("_CON", ReportRunFolder.toFileName("CON", "_"));
        Assert.assertEquals("_Aux", ReportRunFolder.toFileName("Aux...", "_"));
        Assert.assertEquals("_nul.txt", ReportRunFolder.toFileName("nul.txt", "_"));
    }
}
