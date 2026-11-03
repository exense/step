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
import step.cli.CliToolLogging;
import step.cli.ExecuteAutomationPackageTool.ReportOutputMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JUnitReportCreatorTest {

    @Rule
    public final TemporaryFolder reportDir = new TemporaryFolder();

    @Test
    public void unzipsTheReportToTheJunitFolderOfTheRun() throws IOException {
        ReportRunFolder runFolder = new ReportRunFolder(reportDir.getRoot(), LocalDateTime.of(2026, 11, 3, 10, 15, 30));
        List<String> logged = new ArrayList<>();

        JUnitReportCreator.outputZipReport("junit.zip", zip("junit.xml"), List.of("PlanA", "PlanB"), runFolder,
            List.of(ReportOutputMode.file, ReportOutputMode.stdout), logging(logged));

        Path junitFolder = reportDir.getRoot().toPath().resolve("20261103-101530-000000").resolve("junit");
        Assert.assertEquals("<testsuite/>", Files.readString(junitFolder.resolve("junit.xml")));
        Assert.assertEquals("screenshot", Files.readString(junitFolder.resolve("attachments").resolve("1").resolve("page.png")));
        Assert.assertEquals(List.of("The junit report has been saved in " + junitFolder.toUri(), "Junit report of PlanA, PlanB:\n<testsuite/>"), logged);
    }

    /**
     * The controllers of the versions before name the report after the time it was built
     */
    @Test
    public void printsTheReportOfAnOlderControllerWithoutWritingAnything() throws IOException {
        ReportRunFolder runFolder = new ReportRunFolder(reportDir.getRoot());
        List<String> logged = new ArrayList<>();

        JUnitReportCreator.outputZipReport("20241118-173029-281662-junit.zip", zip("20241118-173029-281662-junit.xml"),
            List.of("PlanA"), runFolder, List.of(ReportOutputMode.stdout), logging(logged));

        Assert.assertEquals(List.of("Junit report of PlanA:\n<testsuite/>"), logged);
        Assert.assertFalse(runFolder.getFolder().exists());
    }

    private static byte[] zip(String reportFileName) throws IOException {
        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        try (ZipOutputStream entries = new ZipOutputStream(zip)) {
            entries.putNextEntry(new ZipEntry("attachments/1/page.png"));
            entries.write("screenshot".getBytes(StandardCharsets.UTF_8));
            entries.putNextEntry(new ZipEntry(reportFileName));
            entries.write("<testsuite/>".getBytes(StandardCharsets.UTF_8));
        }
        return zip.toByteArray();
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
