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
import step.core.artefacts.reports.ReportNodeStatus;
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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static step.artefacts.handlers.functions.TokenForecastingExecutionPlugin.getTokenForecastingContext;

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
	public void testWithSelectionCriteria() {
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
		TokenForecastingContext tokenForecastingContext = executePlanWithSpecifiedTokenPools(plan, availableAgentPools);

		assertEquals(Set.of(new AgentPoolRequirementSpec("pool2", 4)),
				Set.copyOf(tokenForecastingContext.getAgentPoolRequirementSpec()));
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

	private void sleep() {
		try {
			Thread.sleep(25);
		} catch (InterruptedException ignored) {}
	}

	private static class Stats {
		AtomicInteger invocations = new AtomicInteger();
		Set<Thread> threads = ConcurrentHashMap.newKeySet();

		void update() {
			invocations.incrementAndGet();
			threads.add(Thread.currentThread());
		}

		void clear() {
			invocations.set(0);
			threads.clear();
		}

		void assertInvocationsAndThreads(int invocations, int threads) {
			assertEquals(invocations, this.invocations.get());
			assertEquals(threads, this.threads.size());
			clear();
		}

		// Because of pooling/thread reuse in the executors, the exact number of threads
		// being used can vary: an upper bound should always be known, but the system
		// may actually be using FEWER threads (precisely because of pooling and reuse).
		// In such cases, still check the number, but using a generous estimate of the expected range
		void assertInvocationsAndThreadsRange(int invocations, int minThreads, int maxThreads) {
			assertEquals(invocations, this.invocations.get());
			assertTrue(String.format("Expected at least %d threads, but actual count=%d", minThreads, this.threads.size()), this.threads.size() >= minThreads);
			assertTrue(String.format("Expected at most %d threads, but actual count=%d", maxThreads, this.threads.size()), this.threads.size() <= maxThreads);
			clear();
		}
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

		Stats stats = new Stats();
		MyFunction function = new MyFunction(input -> {
			sleep();
			stats.update();
			return new Output<>();
		});

		function.addAttribute(AbstractOrganizableObject.NAME, "test");
		plan.setFunctions(List.of(function));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));
		TokenForecastingContext forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);

		// we expect 24 agents because that is the MAXIMUM that is required concurrently
		assertAgentCountPool1(forecast, 24);
		// at least 24 different threads being used (exact value may change because of pool allocation/reuse,
		// usually ~ 25 or 26. See below for another explanation)
		stats.assertInvocationsAndThreadsRange(39, 24, 39);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);
		// for loops cannot forecast exact requirements, so autoNumberOfThreads is returned unchanged
		assertAgentCountPool1(forecast, 42);
		// HOWEVER, all inner loops are re-entrant, so are run single-threaded. Which means that
		// effective concurrency is bound by outer loop, and the "largest" outer loop has 4 iterations=4 threads.
		// As above, numbers of actual threads used may vary slightly (usually observed: around 5 or 6)
		// This is more than 4 because there was a loop (with 3 iterations) before which also needed threads,
		// so pooled threads may be reused, or new ones allocated (exact behavior cannot be predicted)
		stats.assertInvocationsAndThreadsRange(39, 4, 7);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		assertAgentCountPool1(forecast, 1);
		stats.assertInvocationsAndThreads(39, 1);

	}

	@Test
	public void testForInsideThreadGroup() {

		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setUsers(new DynamicValue<>(3));
		IntSequenceDataPool forRange = new IntSequenceDataPool();

		forRange.setStart(new DynamicValue<>(1));
		forRange.setEnd(new DynamicValue<>(4));
		ForBlock forBlock = new ForBlock();
		forBlock.setThreads(new DynamicValue<>(4));
		forBlock.setDataSource(forRange);

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));

		Plan plan = PlanBuilder.create()
			.startBlock(threadGroup) // 3 threads
				.startBlock(forBlock) // 1..4, 4 threads
					.add(testKeyword) // 12 invocations
				.endBlock()
			.endBlock()
		.build();

		Stats stats = new Stats();
		MyFunction function = new MyFunction(input -> {
			sleep();
			stats.update();
			return new Output<>();
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "test");
		plan.setFunctions(List.of(function));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));

		TokenForecastingContext forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		assertAgentCountPool1(forecast, 12);
		stats.assertInvocationsAndThreads(12, 12);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);
		// autoNumberOfThreads = 42, so forecasting returns that.
		assertAgentCountPool1(forecast, 42);
		// outer loop is executed 3 times, inner loop parallelism is disabled because of autoNumberOfThreads setting -> 3 threads
		stats.assertInvocationsAndThreads(12, 3);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		assertAgentCountPool1(forecast, 2);
		stats.assertInvocationsAndThreads(12, 2);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		assertAgentCountPool1(forecast, 1);
		stats.assertInvocationsAndThreads(12, 1);

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

		Stats stats = new Stats();
		MyFunction function = new MyFunction(input -> {
			sleep();
			stats.update();
			return new Output<>();
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "test");
		plan.setFunctions(List.of(function));

		Set<AgentPoolSpec> availableAgentPools = Set.of(
				new AgentPoolSpec("pool1", Map.of("$agenttype", "default", "type", "pool"), 1));
		TokenForecastingContext forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);

		assertAgentCountPool1(forecast, 6);
		stats.assertInvocationsAndThreads(6, 6);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);
		// autoNumberOfThreads = 42, so forecasting returns that.
		assertAgentCountPool1(forecast, 42);
		// outer loop is executed 3 times, inner loop parallelism is disabled because of autoNumberOfThreads setting -> 3 threads
		stats.assertInvocationsAndThreads(6, 3);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		assertAgentCountPool1(forecast, 2);
		stats.assertInvocationsAndThreads(6, 2);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 1);
		assertAgentCountPool1(forecast, 1);
		stats.assertInvocationsAndThreads(6, 1);
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

		Stats stats = new Stats();
		MyFunction function = new MyFunction(input -> {
			sleep();
			stats.update();
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

		TokenForecastingContext forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		assertAgentCountPool1(forecast, 255);
		// can't exactly predict number of effective threads used because of executor pools, but it's usually somewhere around 130 (~70 on build server)
		stats.assertInvocationsAndThreadsRange(255, 35, 255);

		// UC2: L1=3, overriding of execution_threads_auto to 2 -> expecting l3=(1 * 1 * 2) + l2=(1 * 2) + l1=2 => 6 agents, but still 255 invocations

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		assertAgentCountPool1(forecast, 6);
		stats.assertInvocationsAndThreads(255, 2);

		// UC3: L1=1, no overriding of execution_threads_auto -> expecting l3=(11 * 7 * 1) + l2=(7 * 1) + l1=1 => 85 invocations/agents

		tgLevel1.setUsers(new DynamicValue<>(1));
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		assertAgentCountPool1(forecast, 85);
		// again, exact number of threads actually used is hard to tell, usually somewhere around 80
		stats.assertInvocationsAndThreadsRange(85, 25, 85);
	}

	private void assertAgentCountPool1(TokenForecastingContext forecast, int expectedAgentCount) {
		assertEquals(Set.of(new AgentPoolRequirementSpec("pool1", expectedAgentCount)),
				Set.copyOf(forecast.getAgentPoolRequirementSpec()));
	}


	@Test
	public void testTestSetWithTestCases() {

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));
		Stats stats = new Stats();

		MyFunction function = new MyFunction(input -> {
			sleep();
			stats.update();
			return new Output<>();
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "test");

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

		plan.setFunctions(List.of(function));


		TokenForecastingContext forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(6, 1); // default for TestSet is 1 thread
		assertAgentCountPool1(forecast, 1);

		testSet.setThreads(new DynamicValue<>(42));
		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
		stats.assertInvocationsAndThreads(6, 3); // only 3 threads actually in use (= number of TestSet children)
		assertAgentCountPool1(forecast, 3);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 2);
		stats.assertInvocationsAndThreads(6, 2);
		assertAgentCountPool1(forecast, 2);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 3);
		stats.assertInvocationsAndThreads(6, 3);
		assertAgentCountPool1(forecast, 3);

		forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, 42);
		stats.assertInvocationsAndThreads(6, 3);
		assertAgentCountPool1(forecast, 3);

	}

	@Test
	public void testTestSetWithTestCasesAndDynamicAutoThreads() {

		CallFunction testKeyword = FunctionArtefacts.keyword("test");
		testKeyword.setToken(new DynamicValue<>("{\"type\":{\"value\":\"pool\",\"dynamic\":false}}"));
		Stats stats = new Stats();

		MyFunction function = new MyFunction(input -> {
			sleep();
			stats.update();
			return new Output<>();
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "test");

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

		plan.setFunctions(List.of(function));


		autoThreads.setValue(new DynamicValue<>("1"));
		TokenForecastingContext forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
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
		Stats stats = new Stats();

		MyFunction function = new MyFunction(input -> {
			sleep();
			stats.update();
			return new Output<>();
		});
		function.addAttribute(AbstractOrganizableObject.NAME, "test");

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

		plan.setFunctions(List.of(function));


		TokenForecastingContext forecast = executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
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

	private static TokenForecastingContext executePlanWithSpecifiedTokenPools(Plan plan, Set<AgentPoolSpec> availableAgentPools) {
		return executePlanWithSpecifiedTokenPools(plan, availableAgentPools, null);
	}


	private static TokenForecastingContext executePlanWithSpecifiedTokenPools(Plan plan, Set<AgentPoolSpec> availableAgentPools, Integer autoNumberOfThreads) {
		Map<String, String> executionParameters = new HashMap<>();
		if (autoNumberOfThreads != null) {
			executionParameters.put(ThreadPool.EXECUTION_THREADS_AUTO, String.valueOf(autoNumberOfThreads));
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
			executionEngineContext.put(AgentProvisioningDriver.class, new ForecastingTestDriver(agentProvisioningDriverConfiguration));
		}

		@Override
		public void afterExecutionEnd(ExecutionContext context) {
			super.afterExecutionEnd(context);
			this.tokenForecastingContext = getTokenForecastingContext(context);
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
	}


}

