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
import step.core.execution.model.ExecutionAccessor;
import step.core.plans.PlanAccessor;
import step.reporting.JUnit4ReportWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class JUnitXmlReportBuilder {

    private final String executionId;
    private final ExecutionAccessor executionAccessor;
    private final PlanAccessor planAccessor;
    private final ReportNodeAccessor reportNodeAccessor;

    public JUnitXmlReportBuilder(ExecutionEngineContext executionEngineContext, String executionId) {
        this.executionId = executionId;
        this.executionAccessor = executionEngineContext.getExecutionAccessor();
        this.reportNodeAccessor = executionEngineContext.getReportNodeAccessor();
        this.planAccessor = executionEngineContext.getPlanAccessor();
    }

    public String buildJUnitXmlReport() {
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(baos)){
            new JUnit4ReportWriter(true).writeReport(reportNodeAccessor, executionId, writer);
            return new String(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("IO Exception", e);
        }
    }


}
