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
        Assert.assertEquals("Order_an_article", ReportRunFolder.toFileName("Order an article"));
        Assert.assertEquals("Echo", ReportRunFolder.toFileName("Echo..."));
        Assert.assertEquals("_", ReportRunFolder.toFileName(null));
        Assert.assertEquals(50, ReportRunFolder.toFileName("x".repeat(80)).length());
    }
}
