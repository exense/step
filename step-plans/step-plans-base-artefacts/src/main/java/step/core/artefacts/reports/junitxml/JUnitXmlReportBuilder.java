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
package step.core.artefacts.reports.junitxml;

import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.execution.ExecutionEngineContext;
import step.reporting.JUnit4ReportWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class JUnitXmlReportBuilder {

    private final List<String> executionIds;
    private final ReportNodeAccessor reportNodeAccessor;

    public JUnitXmlReportBuilder(ExecutionEngineContext executionEngineContext, List<String> executionIds) {
        this.executionIds = executionIds;
        this.reportNodeAccessor = executionEngineContext.getReportNodeAccessor();
    }

    public Report buildJUnitXmlReport() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(baos)) {
            JUnit4ReportWriter.ReportMetadata reportMetadata;
            JUnit4ReportWriter reportWriter = new JUnit4ReportWriter();
            if (executionIds.size() > 1) {
                reportMetadata = reportWriter.writeMultiReport(reportNodeAccessor, executionIds, writer);
            } else {
                reportMetadata = reportWriter.writeReport(reportNodeAccessor, executionIds.get(0), writer);
            }
            return new Report(reportMetadata, baos.toString());
        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }
    }

    public static class Report {
        private final JUnit4ReportWriter.ReportMetadata metadata;
        private final String content;

        public Report(JUnit4ReportWriter.ReportMetadata fileName, String content) {
            this.metadata = fileName;
            this.content = content;
        }

        public JUnit4ReportWriter.ReportMetadata getMetadata() {
            return metadata;
        }

        public String getContent() {
            return content;
        }
    }

}
