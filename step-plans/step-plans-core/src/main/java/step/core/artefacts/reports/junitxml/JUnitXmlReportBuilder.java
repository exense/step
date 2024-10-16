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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.junitxml.model.TestSuite;
import step.core.artefacts.reports.junitxml.model.TestSuites;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

public class JUnitXmlReportBuilder {

    private final String executionId;
    private final ExecutionAccessor executionAccessor;
    private final PlanAccessor planAccessor;

    public JUnitXmlReportBuilder(ExecutionEngineContext executionEngineContext, String executionId) {
        this.executionId = executionId;
        this.executionAccessor = executionEngineContext.getExecutionAccessor();
        this.planAccessor = executionEngineContext.getPlanAccessor();
    }

    public String buildJUnitXmlReport() {
        Execution execution = executionAccessor.get(executionId);
        Plan plan = planAccessor.get(execution.getPlanId());

        if (plan.getRoot().isTestSet()) {
            // TODO: fill test suites
            TestSuites suites = new TestSuites();
            throw new UnsupportedOperationException("Unsupported root");
        } else {
            TestSuite suite = prepareTestSuite(plan, execution);
            try {
                return buildXmlForSuite(suite);
            } catch (JAXBException ex) {
                throw new RuntimeException("Marshalling exception", ex);
            }
        }
    }

    protected TestSuite prepareTestSuite(Plan plan, Execution execution){
        TestSuite testSuite = new TestSuite();
        testSuite.setTests(testSuite.getTests() + 1);

        if (Set.of(ReportNodeStatus.PASSED).contains(execution.getResult())) {
            // nothing
        } else if (Set.of(ReportNodeStatus.VETOED, ReportNodeStatus.INTERRUPTED, ReportNodeStatus.NORUN, ReportNodeStatus.SKIPPED, ReportNodeStatus.RUNNING).contains(execution.getResult())) {
            testSuite.setSkipped(testSuite.getSkipped() + 1);
        } else if (Set.of(ReportNodeStatus.FAILED).contains(execution.getResult())) {
            testSuite.setFailures(testSuite.getErrors() + 1);
        } else {
            testSuite.setErrors(testSuite.getErrors() + 1);
        }
        if (plan != null) {
            testSuite.setName(plan.getAttribute(AbstractOrganizableObject.NAME));
        } else {
            testSuite.setName("UNKNOWN");
        }
        long executionMs = execution.getEndTime() - execution.getStartTime();
        double executionS = new Long(executionMs).doubleValue() / 1000;
        testSuite.setTime(String.valueOf(executionS));
        return testSuite;
    }

    protected static String buildXmlForSuite(TestSuite testSuite) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(TestSuite.class);
        Marshaller mar = context.createMarshaller();
        mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            mar.marshal(testSuite, os);
            return os.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
