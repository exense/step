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
package step.artefacts.handlers;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.Check;
import step.artefacts.reports.CallFunctionReportNode;
import step.artefacts.reports.CheckReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;

import java.io.StringReader;

import static junit.framework.Assert.assertEquals;

public class CheckHandlerTest extends AbstractArtefactHandlerTest{

    private static final Logger log = LoggerFactory.getLogger(CheckHandlerTest.class);

    @Test
    public void testExpressions(){
        runExpression("output.MyJsonString == \"jsonValue1\"", ReportNodeStatus.PASSED);
        runExpression("output.MyJsonString == \"jsonValueIncorrect\"", ReportNodeStatus.FAILED);

        runExpression("output.MyJsonInt == 888", ReportNodeStatus.PASSED);
        runExpression("output.MyJsonInt == 999", ReportNodeStatus.FAILED);

        runExpression("output.MyJsonBool", ReportNodeStatus.PASSED);
        runExpression("output.MyJsonBool == true", ReportNodeStatus.PASSED);
        runExpression("!output.MyJsonBool", ReportNodeStatus.FAILED);

        runExpression("output.MyJsonNested.MyJsonNestedString == \"jsonValue2\"", ReportNodeStatus.PASSED);
        runExpression("output.MyJsonNested.MyJsonNestedInt == 999", ReportNodeStatus.PASSED);

        runExpression("output.MyJsonArray[0].MyArrayValue == \"arrayValue1\"", ReportNodeStatus.PASSED);
        runExpression("output.MyJsonArray[1].MyArrayValue == \"arrayValue2\"", ReportNodeStatus.PASSED);
        // missing element
        runExpression("output.MyJsonArray[2].MyArrayValue == \"arrayValueIncorrect\"", ReportNodeStatus.TECHNICAL_ERROR);
    }

    private void runExpression(String expression, ReportNodeStatus expectedResult) {
        setupPassed();

        Check a = new Check();
        a.setExpression(new DynamicValue<>(expression, ""));
        execute(a);
        CheckReportNode child = (CheckReportNode) getFirstReportNode();
        log.info(child.getReportAsString());
        assertEquals(expectedResult, child.getStatus());
    }

    private void setupPassed() {
        setupContext();

        CallFunctionReportNode callNode = new CallFunctionReportNode();
        String json = "{\n" +
                "  \"MyJsonString\": \"jsonValue1\",\n" +
                "  \"MyJsonInt\": 888,\n" +
                "  \"MyJsonBool\": true,\n" +
                "  \"MyJsonNested\": {\n" +
                "    \"MyJsonNestedString\": \"jsonValue2\",\n" +
                "    \"MyJsonNestedInt\": 999\n" +
                "  },\n" +
                "  \"MyJsonArray\": [\n" +
                "    {\n" +
                "      \"MyArrayValue\": \"arrayValue1\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"MyArrayValue\": \"arrayValue2\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        JsonObject o = Json.createReader(new StringReader(json)).readObject();
        callNode.setStatus(ReportNodeStatus.PASSED);
        callNode.setOutputObject(o);
        context.getVariablesManager().putVariable(context.getReport(), "callReport", callNode);
        context.getVariablesManager().putVariable(context.getReport(), "output", new UserFriendlyJsonObject(o));
    }
}
