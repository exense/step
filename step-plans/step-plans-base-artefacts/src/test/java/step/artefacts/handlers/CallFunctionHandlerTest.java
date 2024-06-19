/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 * <p>
 * This file is part of STEP
 * <p>
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.artefacts.handlers;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.Check;
import step.artefacts.handlers.functions.TokenForcastingExecutionPlugin;
import step.artefacts.handlers.functions.test.MyFunction;
import step.artefacts.reports.CallFunctionReportNode;
import step.attachments.AttachmentMeta;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.core.reports.Measure;
import step.datapool.DataSetHandle;
import step.engine.plugins.FunctionPlugin;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.grid.io.Attachment;
import step.planbuilder.FunctionArtefacts;
import step.threadpool.ThreadPoolPlugin;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static step.planbuilder.BaseArtefacts.*;

public class CallFunctionHandlerTest extends AbstractFunctionHandlerTest {

	private ExecutionEngine executionEngine;

	@Before
	public void before() {
		executionEngine = ExecutionEngine.builder().withPlugin(new FunctionPlugin()).withPlugin(newMyFunctionTypePlugin())
				.withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).withPlugin(new TokenForcastingExecutionPlugin()).build();
	}

	@After
	public void after() {
		executionEngine.close();
	}
	
	@Test
	public void test() {
		MyFunction function = newPassingFunction();
		Plan plan = newCallFunctionPlan(function);

		PlanRunnerResult result = executionEngine.execute(plan);
		CallFunctionReportNode node = getCallFunctionReportNode(result);

		assertPassingFunctionReportNode(node);
	}

	@Test
	public void testByAttributes() {
		MyFunction function = newPassingFunction();
		CallFunction callFunction = new CallFunction();
		callFunction.setFunction(new DynamicValue<>("{\"name\":\"MyFunction\"}"));
		Plan plan = newPlan(function, callFunction);

		PlanRunnerResult result = executionEngine.execute(plan);
		CallFunctionReportNode node = getCallFunctionReportNode(result);

		assertPassingFunctionReportNode(node);
	}

	@Test
	public void testDrainOutputToMap() {
		MyFunction function = newPassingFunction();
		CallFunction callFunction = FunctionArtefacts.keyword(function.getAttribute(AbstractOrganizableObject.NAME));
		callFunction.setResultMap(new DynamicValue<>("map"));

		Map<String, String> map = new HashMap<>();
		Plan plan = PlanBuilder.create().startBlock(sequence())
				.add(new CheckArtefact(executionContext -> executionContext.getVariablesManager().putVariable(executionContext.getReport(),
						"map", map))).add(callFunction).endBlock().build();
		plan.setFunctions(List.of(function));

		PlanRunnerResult result = executionEngine.execute(plan);

		assertEquals(ReportNodeStatus.PASSED, result.getResult());
		assertEquals("Value1", map.get("Output1"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDrainOutputToDataSetHandle() {
		MyFunction function = newPassingFunction();
		CallFunction callFunction = FunctionArtefacts.keyword(function.getAttribute(AbstractOrganizableObject.NAME));
		callFunction.setResultMap(new DynamicValue<>("dataSet"));

		List<Object> addedRows = new ArrayList<>();
		DataSetHandle dataSetHandle = new DataSetHandle() {

			@Override
			public Object next() {
				return null;
			}

			@Override
			public void addRow(Object row) {
				addedRows.add(row);
			}

		};

		Plan plan = PlanBuilder.create().startBlock(sequence())
				.add(new CheckArtefact(executionContext -> executionContext.getVariablesManager().putVariable(executionContext.getReport(),
						"dataSet", dataSetHandle))).add(callFunction).endBlock().build();
		plan.setFunctions(List.of(function));

		PlanRunnerResult result = executionEngine.execute(plan);

		assertEquals(ReportNodeStatus.PASSED, result.getResult());
		assertEquals("Value1", ((Map<String, String>)addedRows.get(0)).get("Output1"));
	}
	
	@Test
	public void testError() {
		MyFunction function = newFailingFunction();
		Plan plan = newCallFunctionPlan(function);

		PlanRunnerResult result = executionEngine.execute(plan);
		CallFunctionReportNode node = getCallFunctionReportNode(result);
		
		assertEquals("My Error", node.getError().getMsg());
	}
	
	@Test
	public void testSimulation() {
		MyFunction function = newPassingFunction();
		Plan plan = newCallFunctionPlan(function);

		ExecutionParameters executionParameters = new ExecutionParameters(plan, Map.of());
		executionParameters.setMode(ExecutionMode.SIMULATION);

		PlanRunnerResult result = executionEngine.execute(executionParameters);
		CallFunctionReportNode node = getCallFunctionReportNode(result);
		
		assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		assertEquals("{}", node.getOutput());
		assertNull(node.getError());
	}

	@Test
	public void testOutputs() {
		MyFunction function = newFunctionWithOutputs();
		CallFunction callFunction = FunctionArtefacts.keyword(function.getAttribute(AbstractOrganizableObject.NAME));
		callFunction.addChild(check("output.testString == \"test\""));
		callFunction.addChild(check("output.testLong == 111111111111111111L"));
		callFunction.addChild(check("output.testInt == 123"));
		callFunction.addChild(check("output.testBoolean"));
		callFunction.addChild(check("output.testDouble == 123456.789456789"));
		callFunction.addChild(check("output.testBigInteger == 1222222222222222111"));
		callFunction.addChild(check("output.testBigDecimal == 333333333333.44444444444444444444444"));
		callFunction.addChild(check("output.testArray[0] == \"test1\""));
		callFunction.addChild(check("output.nested.nestedInt == 1"));
		callFunction.addChild(assertEqualArtefact("testString", "test"));
		callFunction.addChild(assertEqualArtefact("testLong", "111111111111111111"));
		callFunction.addChild(assertEqualArtefact("testInt", "123"));
		callFunction.addChild(assertEqualArtefact("testBoolean", "true"));
		callFunction.addChild(assertEqualArtefact("testDouble", "123456.789456789"));
		callFunction.addChild(assertEqualArtefact("testBigInteger", "1222222222222222111"));
		callFunction.addChild(assertEqualArtefact("testBigDecimal", "333333333333.44444444444444444444444"));

		Map<String, Object> results = new HashMap<>();
		Plan plan = PlanBuilder.create().startBlock(sequence())
				.add(callFunction)
				.add(new CheckArtefact(executionContext -> {
					Object previous = executionContext.getVariablesManager().getVariable("previous");
					results.put("previous", previous);
				}))
				.endBlock().build();
		plan.setFunctions(List.of(function));

		PlanRunnerResult result = executionEngine.execute(plan);
        assertEquals("PASSED", result.getResult().name());

		Object previous = results.get("previous");
		assertTrue(Map.class.isAssignableFrom(previous.getClass()));
		Map output = (Map) previous;
		assertEquals("test", output.get("testString"));
		assertEquals(111111111111111111L, output.get("testLong"));
		//added as BigDecimal by javax json, same for jarkarta and then deserialized to Long
		assertTrue(123 == ((Number) output.get("testInt")).intValue());
		assertEquals(true, output.get("testBoolean"));
		assertEquals(123456.789456789, ((Number) output.get("testDouble")).doubleValue(),0); //Return a bid deci otherwise
		assertEquals(1222222222222222111L, output.get("testBigInteger")); //ouptut is Long instead of Big Int
		assertEquals(new BigDecimal("333333333333.44444444444444444444444"), output.get("testBigDecimal"));
		assertEquals("test1", ((List<String>) output.get("testArray")).get(0));
		assertEquals(1L, ((Map) output.get("nested")).get("nestedInt")); //Some how return a Long
	}

	private static Plan newCallFunctionPlan(MyFunction function) {
		CallFunction callFunction = FunctionArtefacts.keyword(function.getAttribute(AbstractOrganizableObject.NAME));
		return newPlan(function, callFunction);
	}

	private static Plan newPlan(MyFunction function, CallFunction callFunction) {
		Plan plan = PlanBuilder.create().startBlock(callFunction).endBlock().build();
		plan.setFunctions(List.of(function));
		return plan;
	}

	private static MyFunction newPassingFunction() {
		MyFunction function = new MyFunction(input -> {
			Output<JsonObject> output = new Output<>();

			List<Attachment> attachments = new ArrayList<>();
			Attachment attachment = new Attachment();
			attachment.setName("Attachment1");
			attachment.setHexContent("");
			attachments.add(attachment);
			output.setAttachments(attachments);

			List<Measure> measures = new ArrayList<>();
			measures.add(new Measure("Measure1", 1, 1, null));
			output.setMeasures(measures);

			output.setPayload(Json.createObjectBuilder().add("Output1", "Value1").build());
			return output;
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "MyFunction");
		return function;
	}

	private static MyFunction newFailingFunction() {
		MyFunction function = new MyFunction(input -> {
			Output<JsonObject> output = new Output<>();
			output.setError(new Error(ErrorType.TECHNICAL, "keyword", "My Error", 0, true));
			return output;
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "MyFunction");
		return function;
	}


	private static void assertPassingFunctionReportNode(CallFunctionReportNode node) {
		assertEquals(1, node.getAttachments().size());
		AttachmentMeta attachment = node.getAttachments().get(0);
		assertEquals("Attachment1", attachment.getName());

		assertEquals(2, node.getMeasures().size());

		assertEquals("{\"Output1\":\"Value1\"}", node.getOutput());
		assertEquals(ReportNodeStatus.PASSED, node.getStatus());
	}

	private static MyFunction newFunctionWithOutputs() {
		MyFunction function = new MyFunction(input -> {
			OutputBuilder builder = new OutputBuilder();
			JsonArray array = Json.createArrayBuilder().add("test1").add("test2").build();
			JsonObject nestedInt = Json.createObjectBuilder().add("nestedInt", 1).build();
			builder.getPayloadBuilder()
					.add("testString", "test")
					.add("testLong", 111111111111111111L);
			builder.getPayloadBuilder().add("testInt", 123);
			builder.getPayloadBuilder().add("testBoolean", true)
					.add("testDouble", 123456.789456789)
					.add("testBigInteger", BigInteger.valueOf(1222222222222222111L))
					.add("testBigDecimal", new BigDecimal("333333333333.44444444444444444444444"))
					.add("testArray", array)
					.add("nested", nestedInt);
			return builder.build();
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "MyFunction");
		return function;
	}
}

