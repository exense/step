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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.ForEachBlock;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.artefacts.handlers.functions.test.MyFunction;
import step.artefacts.reports.CallFunctionReportNode;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.datapool.json.JsonArrayDataPoolConfiguration;
import step.engine.plugins.FunctionPlugin;
import step.expressions.ExpressionHandler;
import step.functions.io.Output;
import step.threadpool.ThreadPoolPlugin;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static step.artefacts.handlers.AbstractFunctionHandlerTest.newMyFunctionTypePlugin;
import static step.datapool.DataSources.JSON_ARRAY;

public class ForEachHandlerTest {

	private static final Logger logger = LoggerFactory.getLogger(ForEachHandlerTest.class);
	private ExecutionEngine executionEngine;

	@Before
	public void before() {
		DynamicBeanResolver resolver = new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler()));
		executionEngine = ExecutionEngine.builder().withPlugin(new FunctionPlugin()).withPlugin(newMyFunctionTypePlugin())
				.withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).withPlugin(new TokenForecastingExecutionPlugin()
				).build();
	}

	@After
	public void after() {
		executionEngine.close();
	}

	@Test
	public void testUnprotected() throws IOException {
		ForEachBlock f = new ForEachBlock();

		JsonArrayDataPoolConfiguration configuration = new JsonArrayDataPoolConfiguration();
		configuration.setJson(new DynamicValue<String>("[ {\"a\" : \"va1\", \"b\" : \"vb1\"}, {\"a\" : \"va2\", \"b\" : \"vb2\"}, {\"a\" : 1}, {\"a\" : []}]"));
		configuration.setProtect(new DynamicValue<>(false));

		f.setDataSource(configuration);
		f.setDataSourceType(JSON_ARRAY);
		f.setItem(new DynamicValue<String>("row"));
		f.setGlobalCounter(new DynamicValue<String>("globalCounter"));
		f.setUserItem(new DynamicValue<String>("userId"));

		String argumentStr = "{\"Col1\":{\"value\":\"\",\"dynamic\":true,\"expression\":\"row.a\"}}";
		MyFunction function = newPassingFunctionWithInput();
		CallFunction callFunction = new CallFunction();
		callFunction.setFunction(new DynamicValue<>("{\"name\":\"MyFunction\"}"));
		callFunction.setArgument(new DynamicValue<>(argumentStr));

		Plan plan = PlanBuilder.create().startBlock(f).add(callFunction).endBlock().build();
		plan.setFunctions(List.of(function));
		PlanRunnerResult planRunnerResult = executionEngine.execute(plan);
		planRunnerResult.printTree();
		CallFunctionReportNode node = getFirstCallFunctionReportNode(planRunnerResult);

		assertNull(node.getError());
		assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		assertEquals("{\"Col1\":\"va1\"}", node.getOutput());
		assertEquals("{\"Col1\":\"va1\"}", node.getInput());

	}

	@Test
	public void testProtected() throws IOException {
		ForEachBlock f = new ForEachBlock();

		JsonArrayDataPoolConfiguration configuration = new JsonArrayDataPoolConfiguration();
		configuration.setJson(new DynamicValue<String>("[ {\"a\" : \"va1\", \"b\" : \"vb1\"}, {\"a\" : \"va2\", \"b\" : \"vb2\"}, {\"a\" : 1}, {\"a\" : []}]"));
		configuration.setProtect(new DynamicValue<>(true));

		f.setDataSource(configuration);
		f.setDataSourceType(JSON_ARRAY);
		f.setItem(new DynamicValue<String>("row"));
		f.setGlobalCounter(new DynamicValue<String>("globalCounter"));
		f.setUserItem(new DynamicValue<String>("userId"));

		String argumentStr = "{\"Col1\":{\"value\":\"\",\"dynamic\":true,\"expression\":\"row.a\"}}";
		MyFunction function = newPassingFunctionWithInput();
		CallFunction callFunction = new CallFunction();
		callFunction.setFunction(new DynamicValue<>("{\"name\":\"MyFunction\"}"));
		callFunction.setArgument(new DynamicValue<>(argumentStr));

		Plan plan = PlanBuilder.create().startBlock(f).add(callFunction).endBlock().build();
		plan.setFunctions(List.of(function));
		PlanRunnerResult planRunnerResult = executionEngine.execute(plan);
		planRunnerResult.printTree();
		CallFunctionReportNode node = getFirstCallFunctionReportNode(planRunnerResult);

		assertNull(node.getError());
		assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		assertEquals("{\"Col1\":\"va1\"}", node.getOutput());
		assertEquals("{\"Col1\":\"***row.a***\"}", node.getInput());

	}

	private MyFunction newPassingFunctionWithInput() {
		MyFunction function = new MyFunction(input -> {
			Output<JsonObject> output = new Output<>();

			output.setPayload(Json.createObjectBuilder().add("Col1", input.getPayload().getString("Col1")).build());
			return output;
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "MyFunction");
		return function;
	}

	protected static CallFunctionReportNode getFirstCallFunctionReportNode(PlanRunnerResult result) {
		ReportNode forReport = result.getReportTreeAccessor().getChildren(result.getRootReportNode().getId().toString()).next();
		ReportNode iteration1 = result.getReportTreeAccessor().getChildren(forReport.getId().toString()).next();
		return (CallFunctionReportNode) result.getReportTreeAccessor().getChildren(iteration1.getId().toString()).next();
	}
}

