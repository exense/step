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
package step.localrunner;

import org.junit.Test;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.ThreadGroup;
import step.artefacts.handlers.functions.TokenAutoscalingExecutionPlugin;
import step.artefacts.handlers.functions.test.TestTokenAutoscalingDriver;
import step.artefacts.handlers.functions.autoscaler.TokenAutoscalingConfiguration;
import step.artefacts.handlers.functions.test.MyFunction;
import step.artefacts.handlers.functions.test.MyFunctionType;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.io.Output;
import step.functions.type.FunctionTypeRegistry;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.CompositeFunctionType;
import step.threadpool.ThreadPoolPlugin;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AutoscalerTest {

	@Test
	public void test() throws Exception {
		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setUsers(new DynamicValue<>("1*1", ""));

		ThreadGroup threadGroup2 = new ThreadGroup();
		threadGroup2.setUsers(new DynamicValue<>(2));

		ThreadGroup threadGroup3 = new ThreadGroup();
		threadGroup3.setUsers(new DynamicValue<>("1*3", ""));

		ThreadGroup threadGroup4 = new ThreadGroup();
		threadGroup4.setUsers(new DynamicValue<>(4));

		Plan otherPlan = PlanBuilder.create()
				.startBlock(threadGroup3)
				.add(FunctionArtefacts.keyword("test"))
				.endBlock().build();
		otherPlan.addAttribute(AbstractOrganizableObject.NAME, "MyOtherPlan");

		CallFunction callFunction = FunctionArtefacts.keyword("test");
		Plan compositePlan = PlanBuilder.create()
				.startBlock(threadGroup4)
				.add(callFunction)
				.endBlock().build();

		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.testScenario())
					.startBlock(threadGroup)
						.startBlock(FunctionArtefacts.session())
							.add(FunctionArtefacts.keyword("test"))
							.add(FunctionArtefacts.keyword("test"))
						.endBlock()
					.endBlock()
					.startBlock(threadGroup2)
						.startBlock(FunctionArtefacts.session())
							.add(FunctionArtefacts.keyword("test"))
						.endBlock()
					.endBlock()
					.add(BaseArtefacts.callPlan(otherPlan.getId().toString()))
					.add(FunctionArtefacts.keyword("MyComposite"))
				.endBlock().build();
		plan.setSubPlans(List.of(otherPlan));

		CompositeFunction compositeFunction = new CompositeFunction();
		compositeFunction.addAttribute(AbstractOrganizableObject.NAME, "MyComposite");
		compositeFunction.setPlan(compositePlan);

		MyFunction function = new MyFunction(jsonObjectInput -> new Output<>());
		function.addAttribute(AbstractOrganizableObject.NAME, "test");

		plan.setFunctions(List.of(function, compositeFunction));

		// One matching token pool
		Map<String, Map<String, String>> availableTokenPools = Map.of("pool1", Map.of("$agenttype", "default"));
		TestTokenAutoscalingDriver tokenAutoscalingDriver = executePlanWithSpecifiedTokenPools(plan, availableTokenPools);
		assertEquals(10, (int) tokenAutoscalingDriver.getRequest().requiredNumberOfTokensPerPool.get("pool1"));

		// 2 token pools, one matching
		availableTokenPools = Map.of("pool1", Map.of("$agenttype", "default"), "pool2", Map.of());
		tokenAutoscalingDriver = executePlanWithSpecifiedTokenPools(plan, availableTokenPools);
		assertEquals(10, (int) tokenAutoscalingDriver.getRequest().requiredNumberOfTokensPerPool.get("pool1"));
		assertNull(tokenAutoscalingDriver.getRequest().requiredNumberOfTokensPerPool.get("pool2"));

		// No matching token pool
		TestTokenAutoscalingDriver testDriver = createTestDriver(Map.of());
		PlanRunnerResult result = executePlan(plan, testDriver);
		// !!!!!
		// TODO Fix this. Correct should be TECHNICAL_ERROR!
		// !!!!!
		assertEquals(ReportNodeStatus.NORUN, result.getResult());

		// TODO add test with TestCase as root!
	}

	private static TestTokenAutoscalingDriver executePlanWithSpecifiedTokenPools(Plan plan, Map<String, Map<String, String>> availableTokenPools) {
		TestTokenAutoscalingDriver tokenAutoscalingDriver = createTestDriver(availableTokenPools);
		executePlan(plan, tokenAutoscalingDriver);
		return tokenAutoscalingDriver;
	}

	private static PlanRunnerResult executePlan(Plan plan, TestTokenAutoscalingDriver tokenAutoscalingDriver) {
		try(ExecutionEngine executionEngine = ExecutionEngine.builder().withPlugin(new FunctionPlugin()).withPlugin(new AbstractExecutionEnginePlugin() {
			@Override
			public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
				super.initializeExecutionContext(executionEngineContext, executionContext);
				FunctionTypeRegistry functionTypeRegistry = executionContext.require(FunctionTypeRegistry.class);
				functionTypeRegistry.registerFunctionType(new CompositeFunctionType(null));
				functionTypeRegistry.registerFunctionType(new MyFunctionType());
			}
		}).withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).withPlugin(new TokenAutoscalingExecutionPlugin(tokenAutoscalingDriver)).build()) {
			return executionEngine.execute(plan);
		}
	}

	private static TestTokenAutoscalingDriver createTestDriver(Map<String, Map<String, String>> availableTokenPools) {
		// Create a test driver with a unique pool
		TokenAutoscalingConfiguration configuration = new TokenAutoscalingConfiguration();
		configuration.availableTokenPools = availableTokenPools;
		TestTokenAutoscalingDriver tokenAutoscalingDriver = new TestTokenAutoscalingDriver(configuration);
		return tokenAutoscalingDriver;
	}

}

