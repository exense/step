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
import org.junit.Before;
import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.handlers.functions.TokenAutoscalingExecutionPlugin;
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
import step.grid.io.Attachment;
import step.planbuilder.FunctionArtefacts;
import step.threadpool.ThreadPoolPlugin;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static step.planbuilder.BaseArtefacts.sequence;

public class CallFunctionHandlerTest extends AbstractFunctionHandlerTest {

	private ExecutionEngine executionEngine;

	@Before
	public void before() {
		executionEngine = ExecutionEngine.builder().withPlugin(new FunctionPlugin()).withPlugin(newMyFunctionTypePlugin())
				.withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).withPlugin(new TokenAutoscalingExecutionPlugin()).build();
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
}

