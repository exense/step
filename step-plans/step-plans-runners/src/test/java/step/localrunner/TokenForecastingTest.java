/*******************************************************************************
 * Copyright (C) exense GmbH
 ******************************************************************************/
package step.localrunner;

import org.junit.Test;
import step.artefacts.ThreadGroup;
import step.artefacts.*;
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
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.datapool.json.JsonArrayDataPoolConfiguration;
import step.datapool.sequence.IntSequenceDataPool;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.BasePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.io.Output;
import step.functions.type.FunctionTypeRegistry;
import step.grid.tokenpool.Interest;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.CompositeFunctionType;
import step.threadpool.ThreadPool;
import step.threadpool.ThreadPoolPlugin;

import java.io.PrintWriter;
import java.util.*;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static step.artefacts.handlers.functions.TokenForecastingExecutionPlugin.getTokenForecastingContext;
import static step.artefacts.handlers.functions.TokenSelectionCriteriaMapBuilder.SKIP_AUTO_PROVISIONING;

public class TokenForecastingTest {

	private static class Forecast {
		final List<AgentPoolRequirementSpec> requirements;
		final Set<Map<String, Interest>> criteriaWithoutMatch;

		Forecast(List<AgentPoolRequirementSpec> requirements, Set<Map<String, Interest>> criteriaWithoutMatch) {
			this.requirements = List.copyOf(requirements);
			this.criteriaWithoutMatch = deepCopy(criteriaWithoutMatch);
		}

		Set<Map<String, Interest>> deepCopy(Set<Map<String, Interest>> originalSet) {
			Set<Map<String, Interest>> deepCopy = new HashSet<>();

			for (Map<String, Interest> originalMap : originalSet) {
				deepCopy.add(Map.copyOf(originalMap));
			}
			return Set.copyOf(deepCopy);
		}


	}




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
		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 10)), Set.copyOf(forecast.requirements));

		// 2 token pools, one matching
		availableAgentPools = Set.of(new AgentPoolSpec("pool1", Map.of("$agenttype", "default"), 1), new AgentPoolSpec("pool2", Map.of(), 1));
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 10)), Set.copyOf(forecast.requirements));

		// One matching token pool
		availableAgentPools = Set.of(new AgentPoolSpec("pool1", Map.of("$agenttype", "default"), 2));
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		// 5 agents should be required as each agent has 2 tokens
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 5)), Set.copyOf(forecast.requirements));

		availableAgentPools = Set.of(new AgentPoolSpec("pool1", Map.of("$agenttype", "default"), 3));
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		// 4 agents should be required as each agent has 2 tokens
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 4)), Set.copyOf(forecast.requirements));

	}

	@Test
	public void testWithSelectionCriteria() {
		Plan plan = getPlanWithMultipleSelectionCriteria(false);

		// One matching token pool
		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool1"), 1),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool2"), 1),
				new AgentPoolSpec("pool3", Map.of("$agenttype", "default", "type", "pool3"), 1));

		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 1),
				new AgentPoolRequirementSpec("pool2", 6),
				new AgentPoolRequirementSpec("pool3", 3)),
				Set.copyOf(forecast.requirements));
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
					"\"" + SKIP_AUTO_PROVISIONING + "\":{\"value\":\"true\",\"dynamic\":false}}"));
		} else {
			testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool3\",\"dynamic\":false}}"));
		}

		Plan otherPlan = PlanBuilder.create()
				.startBlock(threadGroup3)
					.add(testKeyword)
				.endBlock().build();
		otherPlan.addAttribute(AbstractOrganizableObject.NAME, "MyOtherPlan");

		CallFunction callFunction = FunctionArtefacts.keyword("test");
		callFunction.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool2\",\"dynamic\":false}," +
				"\"" + SKIP_AUTO_PROVISIONING + "\":{\"expression\":\"variable\",\"dynamic\":true}}"));
		step.artefacts.Set setVariable = BaseArtefacts.set("variable", (skipAutoProvisioning) ? "'true'" : "'false'");

		Plan compositePlan = PlanBuilder.create()
				.startBlock(threadGroup4)
					.add(setVariable)
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
							.add(BaseArtefacts.set((skipAutoProvisioning) ? "route_to_" + SKIP_AUTO_PROVISIONING : "dummy", "'true'"))
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
	public void testWithSelectionCriteriaAndMissingPool() {
		Plan plan = getPlanWithMultipleSelectionCriteria(false);

		// One matching token pool
		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool1"), 1),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool2"), 1));

		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 1),
						new AgentPoolRequirementSpec("pool2", 6)),
				Set.copyOf(forecast.requirements));
		assertEquals(1, forecast.criteriaWithoutMatch.size());
        assertEquals("pool3", forecast.criteriaWithoutMatch.iterator().next().get("type").getSelectionPattern().toString());
	}

	@Test
	public void testWithSelectionCriteriaAndSkipAutoProvisioning() {
		Plan plan = getPlanWithMultipleSelectionCriteria(true);

		// One matching token pool
		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool1"), 1),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool2"), 1),
				new AgentPoolSpec("pool3", Map.of("$agenttype", "default", "type", "pool3"), 1));

		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 1)),
				Set.copyOf(forecast.requirements));
		assertTrue(forecast.criteriaWithoutMatch.isEmpty());
	}


	@Test
	public void testWithNestedSessions() {
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
		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool2", 4)),
				Set.copyOf(forecast.requirements));
	}

	@Test
	public void testWithMultipleMatchingPools() {
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
		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool3", 3), new AgentPoolRequirementSpec("pool1", 1)),
				Set.copyOf(forecast.requirements));

		// If execution_threads_auto is set, it will effectively act as a cap on parallelism (can never be exceeded),
		// so check that the limit takes effect. With parallelism capped to 5, we expect 1 x pool3 and 1 x pool2 (2 tokens)
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 5);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool3", 1), new AgentPoolRequirementSpec("pool2", 1)),
				Set.copyOf(forecast.requirements));

		// Multiple matching token pools. In this case we expect 2 x pool2 (with 4 tokens) and 1 x pool1 (with 2 tokens) for the 10 required tokens
		availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 2),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool"), 4));
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool2", 2), new AgentPoolRequirementSpec("pool1", 1)),
				Set.copyOf(forecast.requirements));

		// Multiple matching token pools. In this case we expect 4 x pool1 (with 3 tokens) for the 10 required tokens. The pool2 with 20 cannot be used
		availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 3),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool"), 20));
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 4)),
				Set.copyOf(forecast.requirements));


		// Multiple matching token pools. In this case we expect 5 x pool1 (with 2 tokens) for the 10 required tokens. The pool2 with 1 token isn't required for this combination
		availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 2),
				new AgentPoolSpec("pool2", Map.of("$agenttype", "default", "type", "pool"), 1));
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", 5)),
				Set.copyOf(forecast.requirements));

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

		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		// just to verify that the execution actually was performed the expected number of times, even if sequentially
		assertEquals(nThreads, actualCalls.get());
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", nThreads)),
				Set.copyOf(forecast.requirements));

		// This value is interpretable as integer, but string-typed, and used to break forecasting before fixing (SED-3832)
		threadGroup.setUsers(new DynamicValue<>("nThreads", ""));

		actualCalls.set(0);
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);
		assertEquals(nThreads, actualCalls.get());
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", nThreads)),
				Set.copyOf(forecast.requirements));
	}

	// We need to sleep a little bit during KW invocations: if the invocations are TOO fast,
	// the thread pool might be able to reuse threads (instead of using new ones) too soon,
	// so our expectations about how many threads should be used will not hold.
	private void sleep() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException ignored) {}
	}

	private static class Stats {
		AtomicInteger invocations = new AtomicInteger();
		AtomicInteger concurrentThreadCounter = new AtomicInteger(0);
		AtomicInteger maxConcurrentThread =  new AtomicInteger(0);

		synchronized void  startInvocation() {
			maxConcurrentThread.updateAndGet(current -> Math.max(current, concurrentThreadCounter.incrementAndGet()));
		}

		synchronized void endInvocation() {
			invocations.incrementAndGet();
			concurrentThreadCounter.decrementAndGet();
		}

		void clear() {
			invocations.set(0);
			concurrentThreadCounter.set(0);
			maxConcurrentThread.set(0);
		}

		void assertInvocationsAndThreads(int invocations, int threads) {
			assertEquals(invocations, this.invocations.get());
			assertEquals(threads, this.maxConcurrentThread.get());
			clear();
		}
	}

	private ForBlock forBlock(int start, int end) {
		ForBlock forBlock = new ForBlock();
		IntSequenceDataPool range = new IntSequenceDataPool();
		range.setStart(new DynamicValue<>(start));
		range.setEnd(new DynamicValue<>(end));
		forBlock.setDataSource(range);
		return forBlock;
	}

	private Stats prepareFunction(Plan plan) {
		Stats stats = new Stats();
		MyFunction function = new MyFunction(input -> {
			stats.startInvocation();
			sleep();
			stats.endInvocation();
			return new Output<>();
		});

		function.addAttribute(AbstractOrganizableObject.NAME, "test");
		plan.setFunctions(List.of(function));
		return  stats;
	}


	@Test
	public void testAdjacentNestedForBlocks() {
		ForBlock outerForBlock1 = forBlock(1, 3);
		outerForBlock1.setThreads(new DynamicValue<>(3));

		ForBlock outerForBlock2 = forBlock(1, 4);
		outerForBlock2.setThreads(new DynamicValue<>(4));

		ForBlock innerForBlock1 = forBlock(1, 5);
		innerForBlock1.setThreads(new DynamicValue<>(5));

		ForBlock innerForBlock2 = forBlock(1, 6);
		innerForBlock2.setThreads(new DynamicValue<>(6));

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

		Stats stats = prepareFunction(plan);

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));
		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);

		stats.assertInvocationsAndThreads(39, 24);
		// we expect 24 agents because that is the MAXIMUM that is required concurrently (second top-level block)
		assertAgentCountPool1(forecast, 24);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);
		// for loops cannot forecast exact requirements, so autoNumberOfThreads is returned unchanged
		assertAgentCountPool1(forecast, 42);
		// HOWEVER, all inner loops are re-entrant, so are run single-threaded. Which means that
		// effective concurrency is bound by outer loop, and the "largest" outer loop has 4 iterations=4 threads.
		stats.assertInvocationsAndThreads(39, 4);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		assertAgentCountPool1(forecast, 2);
		stats.assertInvocationsAndThreads(39, 2);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		assertAgentCountPool1(forecast, 1);
		stats.assertInvocationsAndThreads(39, 1);

	}

	@Test
	public void testForInsideThreadGroup() {

		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setUsers(new DynamicValue<>(3));

		ForBlock forBlock = forBlock(2,5);
		forBlock.setThreads(new DynamicValue<>(4));

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

		Plan plan = PlanBuilder.create()
			.startBlock(threadGroup) // 3 threads
				.startBlock(forBlock) // 2..5, 4 threads
					.add(testKeyword) // 12 invocations
				.endBlock()
			.endBlock()
		.build();

		Stats stats = prepareFunction(plan);

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));

		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		assertAgentCountPool1(forecast, 12);
		stats.assertInvocationsAndThreads(12, 12);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);
		assertAgentCountPool1(forecast, 42);
		// outer loop is executed 42 times, inner loop parallelism is disabled because of autoNumberOfThreads setting -> 42 threads, 42*4 invocations
		stats.assertInvocationsAndThreads(168, 42);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		assertAgentCountPool1(forecast, 2);
		stats.assertInvocationsAndThreads(8, 2);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		assertAgentCountPool1(forecast, 1);
		stats.assertInvocationsAndThreads(4, 1);

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

		Stats stats = prepareFunction(plan);

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));
		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);

		assertAgentCountPool1(forecast, 6);
		stats.assertInvocationsAndThreads(6, 6);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);
		// autoNumberOfThreads = 42, so forecasting returns that.
		assertAgentCountPool1(forecast, 42);
		// outer loop is executed 3 times, inner loop is 1 per thread, but parallelism is disabled because of autoNumberOfThreads setting -> 3 threads and 3 invocations
		stats.assertInvocationsAndThreads(3, 3);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		//outer loop still executed 3 times with inner loop 1 per thread. There is only one inner thread because of autoNumberOfThreads setting -> so 2 threads, 3 invocations
		assertAgentCountPool1(forecast, 2);
		stats.assertInvocationsAndThreads(3, 2);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		//outer loop still executed 3 times with inner loop 1 per thread. There is only one inner thread because of autoNumberOfThreads setting -> so 1 thread, 3 invocations
		assertAgentCountPool1(forecast, 1);
		stats.assertInvocationsAndThreads(3, 1);
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
				.startBlock(forBlock(1,5))
					.add(testKeyword)
				.endBlock()
				.startBlock(tgLevel2)
					.startBlock(forBlock(1,4))
						.add(testKeyword)
					.endBlock()
					.startBlock(tgLevel3)
						.startBlock(forBlock(1,3))
							.add(testKeyword)
						.endBlock()
					.endBlock()
				.endBlock()
			.endBlock()
		.build();

		Stats stats = prepareFunction(plan);

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));


		tgLevel1.setUsers(new DynamicValue<>(3));
		tgLevel2.setUsers(new DynamicValue<>(4));
		tgLevel3.setUsers(new DynamicValue<>(5));

		// GENERAL:
		// Number of INVOCATIONS per level and user: L1=5, L2=4, L3=3
		//
		// UC1: no execution_threads_auto
		// Number of USERS: L1=3, L2=4, L3=5
		// Number of THREADS: L1=3, L2=4, L3=5
		// Expected total invocations: [L1] 3 * 5 = 15 + [L2] (3 * 4) * 4 = 48 + [L3] (3 * 4 * 5) * 3 = 180 => 243.
		// Expected total threads: [L1] 3 + [L2] 3 * 4 = 12 + [L3] 3 * 4 * 5 = 60 => 75
		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(243, 60);
		assertAgentCountPool1(forecast, 75);

		// UC2: execution_threads_auto overridden to 2:
		// Number of THREADS: L1=2, L2=1, L3=1. Note that NO new threads are spawned when inner parallelism is 1 (i.e. only 2 outer threads used)
		// Expected total invocations: [L1] 2 * 5 = 10 + [L2] (2) * 4 = 8 + [L3] (2) * 3 = 6 => 24.
		// Expected total threads: [L1] 2 => 2 (inner loops are not parallelized and don't spawn new threads)
		//However since we have here nested (implicit) sessions in thread groups we still need an agent token for each sessions so : 2+2+2
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		stats.assertInvocationsAndThreads(24, 2);
		assertAgentCountPool1(forecast, 6);

		// Basically the same as above, just explicitly setting the number of threads to 1 instead of 2, so iteration are 5+4+3
		// due to the nested session with need 1 + 1 + 1 agent tokens
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		stats.assertInvocationsAndThreads(12, 1);
		assertAgentCountPool1(forecast, 3);

		// UC3: execution_threads_auto overridden to 7. HOWEVER, it will effectively be limited by the number of USERS of L1 => 3
		// Number of THREADS: L1=7, L2=1, L3=1. Note that NO new threads are spawned when inner parallelism is 1.
		// Expected total invocations: [L1] 7 * 5 = 35 + [L2] (7) * 4 = 28 + [L3] (7) * 3 = 21 => 84.
		// Expected total threads: [L1] 7 => 7 (inner loops are not parallelized and don't spawn new threads)
		// Expected total agent tokens: [L1] 7 => 7 + 7 + 7 (nested sessions require different agent tokens)
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 7);
		stats.assertInvocationsAndThreads(84, 7);
		assertAgentCountPool1(forecast, 21);

		// Set outermost loop to 1 User, but preserve parallelism otherwise
		tgLevel1.setUsers(new DynamicValue<>(1));

		// UC4: no execution_threads_auto
		// Number of THREADS: L1=1, L2=4, L3=5;
		// Expected total invocations: [L1] 1 * 5 = 5 + [L2] (1 * 4) * 4 = 16 + [L3] (1 * 4 * 5) * 3 = 60 => 81.
		// Expected total threads: [L1] 1 + [L2] 1 * 4 = 4 + [L3] 1 * 4 * 5 = 20 => 25
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(81, 20);
		assertAgentCountPool1(forecast, 25);

		// UC5: execution_threads_auto=5, again limited by number of users and therefore EFFECTIVELY 1.
		// Number of THREADS: L1=5, L2=1, L3=1. Note that NO new threads are spawned when inner parallelism is 1.
		// Expected total invocations: [L1] 5 * 5 = 25 + [L2] (5) * 4 = 20 + [L3] (5) * 3 = 15 => 60.
		// Expected total threads: [L1] 5 (inner loops are not parallelized and don't spawn new threads)
		// Expected total agent tokens: [L1] 5 => 5 + 5 + 5 (nested sessions require different agent tokens)
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 5);
		stats.assertInvocationsAndThreads(60, 5);
		assertAgentCountPool1(forecast, 15);
	}

	private void assertAgentCountPool1(Forecast forecast, int expectedAgentCount) {
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", expectedAgentCount)),
				Set.copyOf(forecast.requirements));
	}


	@Test
	public void testTestSetWithTestCases() {

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));

		var testSet = new TestSet();

		// This is a very common structure (e.g. generated by Azure DevOps Test Suites)

		Plan plan = PlanBuilder.create()
				.startBlock(testSet)
					.startBlock(new TestCase())
						.add(testKeyword)
					.endBlock()
					.startBlock(new TestCase())
						.add(testKeyword)
						.add(testKeyword)
					.endBlock()
					.startBlock(new TestCase())
						.add(testKeyword)
						.add(testKeyword)
						.add(testKeyword)
					.endBlock()
				.endBlock()
				.build();

		Stats stats = prepareFunction(plan);


		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(6, 1); // default for TestSet is 1 thread
		assertAgentCountPool1(forecast, 1);

		testSet.setThreads(new DynamicValue<>(42));
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(6, 3);
		assertAgentCountPool1(forecast, 3);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		stats.assertInvocationsAndThreads(6, 2); // TestSet still has 3 children, but now autoNumberOfThreads limits it to 2
		assertAgentCountPool1(forecast, 2);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 3);
		stats.assertInvocationsAndThreads(6, 3);
		assertAgentCountPool1(forecast, 3);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);
		// Only 3 threads actually in use (= number of TestSet children), even if a much higher number of threads is "declared".
		stats.assertInvocationsAndThreads(6, 3);
		assertAgentCountPool1(forecast, 3);

	}

	@Test
	public void testTestSetWithTestCasesAndDynamicAutoThreads() {

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));

		var testSet = new TestSet();
		var autoThreads = new step.artefacts.Set();
		autoThreads.setKey(new DynamicValue<>("execution_threads_auto"));

		Plan plan = PlanBuilder.create()
				.startBlock(new Sequence())
					.add(autoThreads)
					.startBlock(testSet)
						.startBlock(new TestCase())
							.add(testKeyword)
						.endBlock()
						.startBlock(new TestCase())
							.add(testKeyword)
							.add(testKeyword)
						.endBlock()
						.startBlock(new TestCase())
							.add(testKeyword)
							.add(testKeyword)
							.add(testKeyword)
						.endBlock()
					.endBlock()
				.endBlock()
				.build();

		Stats stats = prepareFunction(plan);


		autoThreads.setValue(new DynamicValue<>("1"));
		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(6, 1);
		assertAgentCountPool1(forecast, 1);

		testSet.setThreads(new DynamicValue<>(42)); // effectively ignored because overridden by execution_threads_auto at runtime
		autoThreads.setValue(new DynamicValue<>("5")); // effectively will also be clamped to 3 at runtime
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(6, 3); // only 3 threads actually in use (= number of TestSet children)
		assertAgentCountPool1(forecast, 3);

		autoThreads.setValue(new DynamicValue<>("2"));
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(6, 2);
		assertAgentCountPool1(forecast, 2);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		// autoNumberOfThreads given as parameter to execution is ignored (rather: overwritten by variable Set)
		stats.assertInvocationsAndThreads(6, 2);
		assertAgentCountPool1(forecast, 2);

	}


	@Test
	public void testTestScenario() {

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));

		var testScenario = new TestScenario();

		Plan plan = PlanBuilder.create()
				.startBlock(testScenario)
					.startBlock(new TestCase())
						.add(testKeyword)
					.endBlock()
					.startBlock(new TestCase())
						.add(testKeyword)
						.add(testKeyword)
					.endBlock()
					.startBlock(new TestCase())
						.add(testKeyword)
						.add(testKeyword)
						.add(testKeyword)
					.endBlock()
				.endBlock()
				.build();

		Stats stats = prepareFunction(plan);


		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(6, 3); // default for TestScenario is number of children
		assertAgentCountPool1(forecast, 3);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		stats.assertInvocationsAndThreads(6, 1);
		assertAgentCountPool1(forecast, 1);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		stats.assertInvocationsAndThreads(6, 2);
		assertAgentCountPool1(forecast, 2);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 3);
		stats.assertInvocationsAndThreads(6, 3);
		assertAgentCountPool1(forecast, 3);

		// autoNumberOfThreads will be considered, but clamped to actual number of children of TestScenario (3).
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);
		stats.assertInvocationsAndThreads(6, 3);
		assertAgentCountPool1(forecast, 3);

	}

	@Test
	public void testTestScenarioInTestSet() {

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));

		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.testSet())
					.startBlock(new TestScenario())
						.add(testKeyword)
					.endBlock()
				.endBlock()
				.build();

		Stats stats = prepareFunction(plan);


		Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(1, 1); // default for TestScenario is number of children
		assertAgentCountPool1(forecast, 1);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		stats.assertInvocationsAndThreads(1, 1); // default for TestScenario is number of children
		assertAgentCountPool1(forecast, 1);

		plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.testSet())
				.startBlock(new TestScenario())
				.add(testKeyword)
				.add(testKeyword)
				.endBlock()
				.startBlock(new TestScenario())
				.add(testKeyword)
				.add(testKeyword)
				.endBlock()
				.endBlock()
				.build();

		stats = prepareFunction(plan);


		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(4, 2); // default for TestScenario is number of children
		assertAgentCountPool1(forecast, 2);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		stats.assertInvocationsAndThreads(4, 1); // default for TestScenario is number of children
		assertAgentCountPool1(forecast, 1);
	}

    @Test // SED-4137
    public void testThreadGroupWithDynamicNumbersFromDataset() {
        CallFunction testKeyword = FunctionArtefacts.keyword("test");
        testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

        Set<AgentPoolSpec> availableAgentPools = Set.of(
                new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));

        ForEachBlock foreach = new ForEachBlock();
        foreach.setDataSourceType("json-array");
        JsonArrayDataPoolConfiguration dataSource = new JsonArrayDataPoolConfiguration();
        dataSource.setJson(new DynamicValue<>("[{\"t\": 2, \"i\": 2},{\"t\": 3, \"i\": 3},{\"t\": 5, \"i\": 5},{\"t\": 7, \"i\": 7}]"));
        foreach.setDataSource(dataSource);

        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setUsers(new DynamicValue<>("row.t", ""));
        threadGroup.setIterations(new DynamicValue<>("row.i", ""));

        Plan plan = PlanBuilder.create()
		.startBlock(new TestCase())
			.startBlock(foreach)
				.startBlock(new TestScenario()) // this one isn't strictly necessary and doesn't impact calculations
					.startBlock(threadGroup)
						.add(testKeyword)
					.endBlock()
				.endBlock()
			.endBlock()
		.endBlock()
		.build();

        Stats stats = prepareFunction(plan);

		foreach.setThreads(new DynamicValue<>(1));
		// When autoNumberOfThreads is NOT set, number of iterations is row.i * row.t, i.e. 2*2 + 3*3 + 5*5 + 7*7= 87
		// The outer foreach is defined as single-threaded, which means that the inner threadgroups will run sequentially,
		// so the number of ACTUAL (maximum) concurrent threads is the max. parallelism of the threadgroups, i.e. 7
		// The forecasting will also use this maximum to determine the number of tokens (and therefore, agents, because we set 1 token/agent)
        Forecast forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
        stats.assertInvocationsAndThreads(87, 7);
        assertAgentCountPool1(forecast, 7);

		foreach.setThreads(new DynamicValue<>(4)); // enough to run all 4 data rows of the foreach in parallel
		// Now all inner threadgroups can run in parallel, resulting in 2+3+5+7 = 17 ACTUAL concurrent threads
		// BUT forecasting will use the maximum (7), and multiply by the parallelism, unconditionally
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(87, 17);
		assertAgentCountPool1(forecast, 28); // 7 * 4

		foreach.setThreads(new DynamicValue<>(110)); // again, enough to run all rows in parallel
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(87, 17);
		assertAgentCountPool1(forecast, 770); // 7 * 110, massive overestimation

		// When autoNumberOfThreads IS set, it effectively overrides the number of threads of the outer foreach.
		// Plus, each inner threadgroup is automatically limited to 1 thread, i.e. row.t is (logically) now always 1.
		// Number of executed iterations is therefore ALWAYS 1*2 + 1*3 + 1*5 + 1*7 = 17, regardless of the concrete value of autoNumberOfThreads.
		// The number of ACTUAL concurrent threads is defined by autoNumberOfThreads, but limited by the number of entries (rows) in the foreach (4)
		// However, the forecasting will again take the max. inner parallelism (i.e. 1), and multiply by the outer foreach parallelism, which is now the value of autoNumberOfThreads.
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		stats.assertInvocationsAndThreads(17, 1); // threads = autoNumberOfThreads
		assertAgentCountPool1(forecast, 1);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 3);
		stats.assertInvocationsAndThreads(17, 3);
		assertAgentCountPool1(forecast, 3);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 5);
		stats.assertInvocationsAndThreads(17, 4); // threads = #rows (because autoNumberOfThreads > #rows)
		assertAgentCountPool1(forecast, 5);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);
		stats.assertInvocationsAndThreads(17, 4);
		assertAgentCountPool1(forecast, 42);

	}


	private static Forecast executePlanWithSpecifiedTokenPools(Plan plan, Set<AgentPoolSpec> availableAgentPools) {
		return executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
	}

	private static Forecast executePlanWithSpecifiedTokenPools(Plan plan, Set<AgentPoolSpec> availableAgentPools, Integer autoNumberOfThreads) {
		Map<String, String> executionParameters = new HashMap<>();
		if (autoNumberOfThreads != null) {
			executionParameters.put(ThreadPool.EXECUTION_THREADS_AUTO, String.valueOf(autoNumberOfThreads));
		}
		ForecastingTestPlugin forecastingTestPlugin = new ForecastingTestPlugin(availableAgentPools);
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
				.withPlugin(forecastingTestPlugin)
				.build()) {
			var result = executionEngine.execute(plan, executionParameters);
			try {
				result.waitForExecutionToTerminate();
				if (!result.getResult().equals(ReportNodeStatus.PASSED)) {
					try {
						result.printTree(new PrintWriter(System.err), true, true);
					} catch (Exception oops) {
						oops.printStackTrace();
					}
				}
				assertEquals(ReportNodeStatus.PASSED, result.getResult());
			} catch (InterruptedException|TimeoutException e) {
				throw new RuntimeException(e);
			}
		}
		return forecastingTestPlugin.forecast;
	}

	public static class ForecastingTestPlugin extends AbstractExecutionEnginePlugin {

		Forecast forecast;
		Set<AgentPoolSpec> availableTokenPools;

		public ForecastingTestPlugin(Set<AgentPoolSpec> availableTokenPools) {
			this.availableTokenPools = availableTokenPools;
		}

		@Override
		public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {
			super.initializeExecutionEngineContext(parentContext, executionEngineContext);
			AgentProvisioningDriverConfiguration agentProvisioningDriverConfiguration = new AgentProvisioningDriverConfiguration();
			agentProvisioningDriverConfiguration.availableAgentPools = availableTokenPools;
			executionEngineContext.put(AgentProvisioningDriver.class, new ForecastingTestDriver(agentProvisioningDriverConfiguration));
		}

		@Override
		public void provisionRequiredResources(ExecutionContext context) {
			super.provisionRequiredResources(context);
			var tokenForecastingContext = getTokenForecastingContext(context);
			var requirements = tokenForecastingContext.getAgentPoolRequirementSpec();
			this.forecast = new Forecast(requirements, tokenForecastingContext.getCriteriaWithoutMatch());
		}
	}

	public static class ForecastingTestDriver implements AgentProvisioningDriver {

		AgentProvisioningDriverConfiguration agentProvisioningDriverConfiguration;

		public ForecastingTestDriver(AgentProvisioningDriverConfiguration agentProvisioningDriverConfiguration) {
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

		@Override
		public void registerRemoteAgentPoolSpecs(Set<AgentPoolSpec> agentPoolSpecs) {

		}
	}


}

