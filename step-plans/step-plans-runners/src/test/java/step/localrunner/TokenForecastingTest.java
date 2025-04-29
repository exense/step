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
import step.artefacts.ThreadGroup;
import step.artefacts.*;
import step.artefacts.handlers.functions.TokenForecastingContext;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.artefacts.handlers.functions.test.MyFunction;
import step.artefacts.handlers.functions.test.MyFunctionType;
import step.core.accessors.AbstractOrganizableObject;
import step.core.agents.provisioning.AgentPoolRequirementSpec;
import step.core.agents.provisioning.AgentPoolSpec;
import step.core.agents.provisioning.driver.AgentProvisioningDriver;
import step.core.agents.provisioning.driver.AgentProvisioningDriverConfiguration;
import step.core.agents.provisioning.driver.AgentProvisioningRequest;
import step.core.agents.provisioning.driver.AgentProvisioningStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.datapool.sequence.IntSequenceDataPool;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.BasePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.io.Output;
import step.functions.type.FunctionTypeRegistry;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.CompositeFunctionType;
import step.threadpool.ThreadPool;
import step.threadpool.ThreadPoolPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static step.artefacts.handlers.functions.TokenForecastingExecutionPlugin.getTokenForecastingContext;
import static step.artefacts.handlers.functions.TokenSelectionCriteriaMapBuilder.SKIP_AUTO_PROVISIONING;

public class TokenForecastingTest {

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
		Plan plan = getPlanWithMultipleSelectionCriteria(false);

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

	private static Plan getPlanWithMultipleSelectionCriteria(boolean skipAutoProvisioning) {
		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setUsers(new DynamicValue<>("1*1", ""));

		ThreadGroup threadGroup2 = new ThreadGroup();
		threadGroup2.setUsers(new DynamicValue<>(2));

		ThreadGroup threadGroup3 = new ThreadGroup();
		threadGroup3.setUsers(new DynamicValue<>("1*3", ""));

		ThreadGroup threadGroup4 = new ThreadGroup();
		threadGroup4.setUsers(new DynamicValue<>(4));

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		if (skipAutoProvisioning) {
			testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool3\",\"dynamic\":false}," +
					"\"" + SKIP_AUTO_PROVISIONING + "\":{\"value\":\"\",\"dynamic\":false}}"));
		} else {
			testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool3\",\"dynamic\":false}}"));
		}

		Plan otherPlan = PlanBuilder.create()
				.startBlock(threadGroup3)
					.add(testKeyword)
				.endBlock().build();
		otherPlan.addAttribute(AbstractOrganizableObject.NAME, "MyOtherPlan");

		CallFunction callFunction = FunctionArtefacts.keyword("test");
		if (skipAutoProvisioning) {
			callFunction.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool2\",\"dynamic\":false}," +
					"\"" + SKIP_AUTO_PROVISIONING + "\":{\"value\":\"\",\"dynamic\":false}}"));
		} else {
			callFunction.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool2\",\"dynamic\":false}}"));
		}
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
							.add(BaseArtefacts.set((skipAutoProvisioning) ? "route_to_" + SKIP_AUTO_PROVISIONING : "dummy", "''"))
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
		return plan;
	}

	@Test
	public void testWithSelectionCriteriaAndMissingPool() throws Exception {
		Plan plan = getPlanWithMultipleSelectionCriteria(false);

		// One matching token pool
		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool1"), 1),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool2"), 1));

		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 1),
						new AgentPoolRequirementSpec("pool2", 6)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(1, tokenForecastingContext.getCriteriaWithoutMatch().size());
		assertEquals("pool3", tokenForecastingContext.getCriteriaWithoutMatch().stream().findFirst().get().values().stream().findFirst().get().getSelectionPattern().toString());
	}

	@Test
	public void testWithSelectionCriteriaAndSkipAutoProvisioning() throws Exception {
		Plan plan = getPlanWithMultipleSelectionCriteria(true);

		// One matching token pool
		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool1"), 1),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool2"), 1),
				new AgentPoolSpec("pool3", Map.of("$agenttype", "default", "type", "pool3"), 1));

		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 1)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertTrue(tokenForecastingContext.getCriteriaWithoutMatch().isEmpty());
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

	@Test
	public void testWithMultipleMatchingPools() throws Exception {
		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setUsers(new DynamicValue<>(10));

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

		Plan plan = PlanBuilder.create()
				.startBlock(threadGroup)
				.add(testKeyword)
				.endBlock().build();
		MyFunction function = new MyFunction(jsonObjectInput -> new Output<>());
		function.addAttribute(AbstractOrganizableObject.NAME, "test");
		plan.setFunctions(List.of(function));

		// Multiple matching token pools. In this case we expect 3 x pool3 (with 3 tokens) and 1 x pool1 (with 1 token) for the 10 required tokens
		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool"), 2),
				new AgentPoolSpec("pool3", Map.of("$agenttype", "default", "type", "pool"), 3));
		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool3", 3), new AgentPoolRequirementSpec("pool1", 1)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));

		// If execution_threads_auto is set, it will effectively act as a cap on parallelism (can never be exceeded),
		// so check that the limit takes effect. With parallelism capped to 5, we expect 1 x pool3 and 1 x pool2 (2 tokens)
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 5);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool3", 1), new AgentPoolRequirementSpec("pool2", 1)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));

		// Multiple matching token pools. In this case we expect 2 x pool2 (with 4 tokens) and 1 x pool1 (with 2 token) for the 10 required tokens
		availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 2),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool"), 4));
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool2", 2), new AgentPoolRequirementSpec("pool1", 1)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));

		// Multiple matching token pools. In this case we expect 4 x pool1 (with 3 tokens) for the 10 required tokens. The pool2 with 20 cannot be used
		availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 3),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool"), 20));
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 4)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));


		// Multiple matching token pools. In this case we expect 5 x pool1 (with 2 tokens) for the 10 required tokens. The pool1 with 1 token isn't required for this combination
		availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 2),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool"), 1));
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 5)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));

	}

	@Test
	public void testWithRuntimeVariables() {
		int nThreads = 7;
		step.artefacts.Set setVariable = new step.artefacts.Set();
		setVariable.setKey(new DynamicValue<>("nThreads"));
		setVariable.setValue(new DynamicValue<>("" + nThreads));
		ThreadGroup threadGroup = new ThreadGroup();
		// correctly typed as integer
		threadGroup.setUsers(new DynamicValue<>("nThreads.toInteger()", ""));

		CallFunction keyword = FunctionArtefacts.keyword("test");

		Plan plan = PlanBuilder.create()
			.startBlock(BaseArtefacts.testCase())
				.add(setVariable)
				.startBlock(threadGroup)
					.add(keyword)
				.endBlock()
			.endBlock()
			.build();

		AtomicInteger actualCalls = new AtomicInteger();

		MyFunction function = new MyFunction(input -> {
			actualCalls.incrementAndGet();
			return new Output<>();
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "test");
		plan.setFunctions(List.of(function));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));

		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		// just to verify that the execution actually was performed the expected number of times, even if sequentially
		assertEquals(nThreads, actualCalls.get());
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", nThreads)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));

		// This value is interpretable as integer, but string-typed, and used to break forecasting before fixing (SED-3832)
		threadGroup.setUsers(new DynamicValue<>("nThreads", ""));

		actualCalls.set(0);
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		assertEquals(nThreads, actualCalls.get());
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", nThreads)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
	}

	@Test
	public void testAdjacentNestedForBlocks() {
		IntSequenceDataPool outerRange1 = new IntSequenceDataPool();
		outerRange1.setStart(new DynamicValue<>(1));
		outerRange1.setEnd(new DynamicValue<>(3));
		ForBlock outerForBlock1 = new ForBlock();
		outerForBlock1.setThreads(new DynamicValue<>(3));
		outerForBlock1.setDataSource(outerRange1);

		IntSequenceDataPool outerRange2 = new IntSequenceDataPool();
		outerRange2.setStart(new DynamicValue<>(1));
		outerRange2.setEnd(new DynamicValue<>(4));
		ForBlock outerForBlock2 = new ForBlock();
		outerForBlock2.setThreads(new DynamicValue<>(4));
		outerForBlock2.setDataSource(outerRange2);

		IntSequenceDataPool innerRange1 = new IntSequenceDataPool();
		innerRange1.setStart(new DynamicValue<>(1));
		innerRange1.setEnd(new DynamicValue<>(5));
		ForBlock innerForBlock1 = new ForBlock();
		innerForBlock1.setThreads(new DynamicValue<>(5));
		innerForBlock1.setDataSource(innerRange1);

		IntSequenceDataPool innerRange2 = new IntSequenceDataPool();
		innerRange2.setStart(new DynamicValue<>(1));
		innerRange2.setEnd(new DynamicValue<>(6));
		ForBlock innerForBlock2 = new ForBlock();
		innerForBlock2.setThreads(new DynamicValue<>(6));
		innerForBlock2.setDataSource(innerRange2);

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.sequence())
					.startBlock(outerForBlock1) // 1..3, 3 threads
						.startBlock(innerForBlock1) // 1..5, 5 threads
							.add(testKeyword) // 15 invocations
						.endBlock()
					.endBlock()
					.startBlock(outerForBlock2) // 1..4, 4 threads
						.startBlock(innerForBlock2) // 1..6, 6 threads
							.add(testKeyword)  // 24 invocations
						.endBlock()
					.endBlock()
				.endBlock()
		.build();

		AtomicInteger invocations = new AtomicInteger();
		MyFunction function = new MyFunction(input -> {
			invocations.incrementAndGet();
			return new Output<>();
		});

		function.addAttribute(AbstractOrganizableObject.NAME, "test");
		plan.setFunctions(List.of(function));
		plan.setFunctions(List.of(function));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));
		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 24)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(39, invocations.get());

		invocations.set(0);
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 42)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(39, invocations.get());

		invocations.set(0);
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 1)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(39, invocations.get());

	}

	@Test
	public void testForInsideThreadGroup() {

		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setUsers(new DynamicValue<>(2));
		IntSequenceDataPool forRange = new IntSequenceDataPool();

		forRange.setStart(new DynamicValue<>(1));
		forRange.setEnd(new DynamicValue<>(3));
		ForBlock forBlock = new ForBlock();
		forBlock.setThreads(new DynamicValue<>(3));
		forBlock.setDataSource(forRange);

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

		Plan plan = PlanBuilder.create()
			.startBlock(threadGroup) // 2 threads
				.startBlock(forBlock) // 1..3, 3 threads
					.add(testKeyword) // 6 invocations
				.endBlock()
			.endBlock()
		.build();

		AtomicInteger invocations = new AtomicInteger();
		MyFunction function = new MyFunction(input -> {
			invocations.incrementAndGet();
			return new Output<>();
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "test");
		plan.setFunctions(List.of(function));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));
		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 6)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(6, invocations.get());

		invocations.set(0);
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 42)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(6, invocations.get());

		invocations.set(0);
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 1)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(6, invocations.get());

	}

	@Test
	public void testThreadGroupInsideFor() {

		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setUsers(new DynamicValue<>(2));
		IntSequenceDataPool forRange = new IntSequenceDataPool();

		forRange.setStart(new DynamicValue<>(1));
		forRange.setEnd(new DynamicValue<>(3));
		ForBlock forBlock = new ForBlock();
		forBlock.setThreads(new DynamicValue<>(3));
		forBlock.setDataSource(forRange);

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

		Plan plan = PlanBuilder.create()
			.startBlock(forBlock) // 1..3, 3 threads
				.startBlock(threadGroup) // 2 threads
					.add(testKeyword) // 6 invocations
				.endBlock()
			.endBlock()
		.build();

		AtomicInteger invocations = new AtomicInteger();
		MyFunction function = new MyFunction(input -> {
			invocations.incrementAndGet();
			return new Output<>();
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "test");
		plan.setFunctions(List.of(function));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));
		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 6)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(6, invocations.get());

		invocations.set(0);
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 42)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(6, invocations.get());

		invocations.set(0);
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 1)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(6, invocations.get());

	}

	@Test
	public void testNestedThreadGroups() {

		ThreadGroup tgLevel1 = new ThreadGroup();
		ThreadGroup tgLevel2 = new ThreadGroup();
		ThreadGroup tgLevel3 = new ThreadGroup();

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

		Plan plan = PlanBuilder.create()
			.startBlock(tgLevel1)
				.add(testKeyword)
				.startBlock(tgLevel2)
					.add(testKeyword)
					.startBlock(tgLevel3)
						.add(testKeyword)
					.endBlock()
				.endBlock()
			.endBlock()
		.build();

		AtomicInteger invocations = new AtomicInteger();
		MyFunction function = new MyFunction(input -> {
			invocations.incrementAndGet();
            return new Output<>();
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "test");
		plan.setFunctions(List.of(function));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));


		tgLevel1.setUsers(new DynamicValue<>(3));
		tgLevel2.setUsers(new DynamicValue<>(7));
		tgLevel3.setUsers(new DynamicValue<>(11));
		// UC1: L1=3, no overriding of execution_threads_auto -> expecting l3=(11 * 7 * 3) + l2=(7 * 3) + l1=3 => 255 invocations/agents
		invocations.set(0);
		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 255)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(255, invocations.get());

		// UC2: L1=3, overriding of execution_threads_auto to 2 -> expecting l3=(1 * 1 * 2) + l2=(1 * 2) + l1=2 => 6 agents, but still 255 invocations
		invocations.set(0);
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 6)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(255, invocations.get());

		// UC3: L1=1, no overriding of execution_threads_auto -> expecting l3=(11 * 7 * 1) + l2=(7 * 1) + l1=1 => 85 invocations/agents
		invocations.set(0);
		tgLevel1.setUsers(new DynamicValue<>(1));
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 85)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(85, invocations.get());

		// UC4: L1=1, overriding execution_threads_auto to 2 -> expecting l3=(1 * 2 * 1) + l2=(2 * 1) + l1=1 => 5 agents, but 85 invocations
		// Note how the execution_threads_auto now overrides the threads at the SECOND level, not the first (because L1 is not parallelized).
		// If it was overriding at L1, it would be l3=(1 * 1 * 2) + l2=(1*2) + l1=2 => 6 agents (same as UC2).
		invocations.set(0);
		tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 5)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
		assertEquals(85, invocations.get());

	}


	private static TokenForecastingContext executePlanWithSpecifiedTokenPools(Plan plan, Set<AgentPoolSpec> availableAgentPools) {
		return executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
	}


	private static TokenForecastingContext executePlanWithSpecifiedTokenPools(Plan plan, Set<AgentPoolSpec> availableAgentPools, Integer execution_threads_auto) {
		Map<String, String> executionParameters = new HashMap<>();
		if (execution_threads_auto != null) {
			executionParameters.put(ThreadPool.EXECUTION_THREADS_AUTO, String.valueOf(execution_threads_auto));
		}
		ForcastingTestPlugin forcastingTestPlugin = new ForcastingTestPlugin(availableAgentPools);
		try (ExecutionEngine executionEngine = ExecutionEngine.builder()
				.withPlugin(new BasePlugin())
				.withPlugin(new FunctionPlugin())
				.withPlugin(new AbstractExecutionEnginePlugin() {
					@Override
					public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
						super.initializeExecutionContext(executionEngineContext, executionContext);
						FunctionTypeRegistry functionTypeRegistry = executionContext.require(FunctionTypeRegistry.class);
						functionTypeRegistry.registerFunctionType(new CompositeFunctionType(null));
						functionTypeRegistry.registerFunctionType(new MyFunctionType());
					}
				}).withPlugin(new ThreadPoolPlugin())
				.withPlugin(new BaseArtefactPlugin())
				.withPlugin(new TokenForecastingExecutionPlugin())
				.withPlugin(forcastingTestPlugin)
				.build()) {
			executionEngine.execute(plan, executionParameters);
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
			AgentProvisioningDriverConfiguration agentProvisioningDriverConfiguration = new AgentProvisioningDriverConfiguration();
			agentProvisioningDriverConfiguration.availableAgentPools = availableTokenPools;
			executionEngineContext.put(AgentProvisioningDriver.class, new ForcastingTestDriver(agentProvisioningDriverConfiguration));
		}

		@Override
		public void afterExecutionEnd(ExecutionContext context) {
			super.afterExecutionEnd(context);
			this.tokenForecastingContext = getTokenForecastingContext(context);
		}
	}

	public static class ForcastingTestDriver implements AgentProvisioningDriver {

		AgentProvisioningDriverConfiguration agentProvisioningDriverConfiguration;

		public ForcastingTestDriver(AgentProvisioningDriverConfiguration agentProvisioningDriverConfiguration) {
			this.agentProvisioningDriverConfiguration = agentProvisioningDriverConfiguration;
		}

		@Override
		public AgentProvisioningDriverConfiguration getConfiguration() {
			return agentProvisioningDriverConfiguration;
		}

		@Override
		public String initializeTokenProvisioningRequest(AgentProvisioningRequest request) {
			return null;
		}

		@Override
		public AgentProvisioningStatus executeTokenProvisioningRequest(String provisioningRequestId) {
			return new AgentProvisioningStatus();
		}

		@Override
		public AgentProvisioningStatus getTokenProvisioningStatus(String provisioningRequestId) {
			return null;
		}

		@Override
		public void deprovisionTokens(String provisioningRequestId) {

		}
	}


}

