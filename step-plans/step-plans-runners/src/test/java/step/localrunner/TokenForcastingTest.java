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
import step.artefacts.FunctionGroup;
import step.artefacts.ThreadGroup;
import step.artefacts.handlers.functions.TokenForcastingExecutionPlugin;
import step.artefacts.handlers.functions.TokenForecastingContext;
import step.artefacts.handlers.functions.autoscaler.*;
import step.artefacts.handlers.functions.test.MyFunction;
import step.artefacts.handlers.functions.test.MyFunctionType;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
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
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static step.artefacts.handlers.functions.TokenForcastingExecutionPlugin.getTokenForecastingContext;

public class TokenForcastingTest {

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
		Set<AgentPoolSpec> availableAgentPools = Set.of(new AgentPoolSpec("pool1", Map.of("$agenttype", "default"), 1));
		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 10)), Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));

		// 2 token pools, one matching
		availableAgentPools = Set.of(new AgentPoolSpec("pool1", Map.of("$agenttype", "default"), 1), new AgentPoolSpec("pool2", Map.of(), 1));
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 10)), Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));

		// One matching token pool
		availableAgentPools = Set.of(new AgentPoolSpec("pool1", Map.of("$agenttype", "default"), 2));
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		// 5 agents should be required as each agent has 2 tokens
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 5)), Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));

		availableAgentPools = Set.of(new AgentPoolSpec("pool1", Map.of("$agenttype", "default"), 3));
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		// 4 agents should be required as each agent has 2 tokens
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 4)), Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));

	}

	@Test
	public void testWithSelectionCriteria() throws Exception {
		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setUsers(new DynamicValue<>("1*1", ""));

		ThreadGroup threadGroup2 = new ThreadGroup();
		threadGroup2.setUsers(new DynamicValue<>(2));

		ThreadGroup threadGroup3 = new ThreadGroup();
		threadGroup3.setUsers(new DynamicValue<>("1*3", ""));

		ThreadGroup threadGroup4 = new ThreadGroup();
		threadGroup4.setUsers(new DynamicValue<>(4));

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool3\",\"dynamic\":false}}"));

		Plan otherPlan = PlanBuilder.create()
				.startBlock(threadGroup3)
					.add(testKeyword)
				.endBlock().build();
		otherPlan.addAttribute(AbstractOrganizableObject.NAME, "MyOtherPlan");

		CallFunction callFunction = FunctionArtefacts.keyword("test");
		callFunction.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool2\",\"dynamic\":false}}"));
		Plan compositePlan = PlanBuilder.create()
				.startBlock(threadGroup4)
					.add(callFunction)
				.endBlock().build();

		FunctionGroup session = FunctionArtefacts.session();
		session.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool1\",\"dynamic\":false}}"));
		FunctionGroup session2 = FunctionArtefacts.session();
		session2.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool2\",\"dynamic\":false}}"));
		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.testScenario())
					.startBlock(threadGroup)
						.startBlock(session)
							.add(FunctionArtefacts.keyword("test"))
							.add(FunctionArtefacts.keyword("test"))
						.endBlock()
					.endBlock()
					.startBlock(threadGroup2)
						.startBlock(session2)
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
		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool1"), 1),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool2"), 1),
				new AgentPoolSpec("pool3", Map.of("$agenttype", "default", "type", "pool3"), 1));

		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 1),
				new AgentPoolRequirementSpec("pool2", 6),
				new AgentPoolRequirementSpec("pool3", 3)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
	}


	@Test
	public void testWithNestedSessions() throws Exception {
		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setUsers(new DynamicValue<>("1*4", ""));

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool3\",\"dynamic\":false}}"));

		FunctionGroup session = FunctionArtefacts.session();
		session.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool2\",\"dynamic\":false}}"));

		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.testCase())
					.startBlock(threadGroup)
						.startBlock(session)
							.startBlock(BaseArtefacts.for_(1, 5))
								.add(testKeyword)
								.add(testKeyword)
							.endBlock()
						.endBlock()
				.endBlock()
				.endBlock().build();
		MyFunction function = new MyFunction(jsonObjectInput -> new Output<>());
		function.addAttribute(AbstractOrganizableObject.NAME, "test");
		plan.setFunctions(List.of(function));

		// One matching token pool
		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool1"), 1),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool2"), 1),
				new AgentPoolSpec("pool3", Map.of("$agenttype", "default", "type", "pool3"), 1));
		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool2", 4)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
	}

	private static TokenForecastingContext executePlanWithSpecifiedTokenPools(Plan plan, Set<AgentPoolSpec> availableAgentPools) {
		ForcastingTestPlugin forcastingTestPlugin = new ForcastingTestPlugin(availableAgentPools);
		try(ExecutionEngine executionEngine = ExecutionEngine.builder().withPlugin(new FunctionPlugin()).withPlugin(new AbstractExecutionEnginePlugin() {
			@Override
			public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
				super.initializeExecutionContext(executionEngineContext, executionContext);
				FunctionTypeRegistry functionTypeRegistry = executionContext.require(FunctionTypeRegistry.class);
				functionTypeRegistry.registerFunctionType(new CompositeFunctionType(null));
				functionTypeRegistry.registerFunctionType(new MyFunctionType());
				}
		}).withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).withPlugin(new TokenForcastingExecutionPlugin())
				.withPlugin(forcastingTestPlugin).build()) {
			executionEngine.execute(plan);
		}
		return forcastingTestPlugin.tokenForecastingContext;
	}

	public static class ForcastingTestPlugin extends AbstractExecutionEnginePlugin {

		public TokenForecastingContext tokenForecastingContext;
		Set<AgentPoolSpec> availableTokenPools;

		public ForcastingTestPlugin(Set<AgentPoolSpec> availableTokenPools) {
			this.availableTokenPools = availableTokenPools;
		}

		@Override
		public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {
			super.initializeExecutionEngineContext(parentContext, executionEngineContext);
			TokenAutoscalingConfiguration tokenAutoscalingConfiguration = new TokenAutoscalingConfiguration();
			tokenAutoscalingConfiguration.availableAgentPools = availableTokenPools;
			executionEngineContext.put(TokenAutoscalingDriver.class, new ForcastingTestDriver(tokenAutoscalingConfiguration));
		}

		@Override
		public void afterExecutionEnd(ExecutionContext context) {
			super.afterExecutionEnd(context);
			this.tokenForecastingContext = getTokenForecastingContext(context);
		}
	}

	public static class ForcastingTestDriver implements TokenAutoscalingDriver {

		TokenAutoscalingConfiguration tokenAutoscalingConfiguration;

		public ForcastingTestDriver(TokenAutoscalingConfiguration tokenAutoscalingConfiguration) {
			this.tokenAutoscalingConfiguration = tokenAutoscalingConfiguration;
		}

		@Override
		public TokenAutoscalingConfiguration getConfiguration() {
			return tokenAutoscalingConfiguration;
		}

		@Override
		public String initializeTokenProvisioningRequest(TokenProvisioningRequest request) {
			return null;
		}

		@Override
		public void executeTokenProvisioningRequest(String provisioningRequestId) {

		}

		@Override
		public TokenProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId) {
			return null;
		}

		@Override
		public void deprovisionTokens(String provisioningRequestId) {

		}
	}


}

