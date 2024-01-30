package step.artefacts.handlers;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import step.artefacts.Aggregator;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.Comparator;
import step.artefacts.Filter;
import step.artefacts.FilterType;
import step.artefacts.PerformanceAssert;
import step.artefacts.Set;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plans.runner.PlanRunnerResultAssert;
import step.engine.plugins.FunctionPlugin;
import step.engine.plugins.LocalFunctionPlugin;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.plans.assertions.PerformanceAssertPlugin;
import step.threadpool.ThreadPoolPlugin;

public class PerformanceAssertHandlerTest extends AbstractKeyword {

	private List<Filter> filterMyMeasure1;
	private Filter filterMyMeasure1Equals;
	private Filter filterMyMeasure2Regex;
	private Filter filterKeywordMeasure;
	private Filter invalidFilter;
	private ExecutionEngine engine;
	
	@Before
	public void before() {
		filterMyMeasure1 = new ArrayList<>();
		filterMyMeasure1Equals = new Filter(AbstractOrganizableObject.NAME, "myMeasure1", FilterType.EQUALS);
		filterMyMeasure1.add(filterMyMeasure1Equals);
		filterMyMeasure2Regex = new Filter(AbstractOrganizableObject.NAME, ".*Measure2*", FilterType.REGEX);
		filterKeywordMeasure = new Filter(AbstractOrganizableObject.NAME, "TestKeywordWithMeasurements", FilterType.EQUALS);
		invalidFilter = new Filter(AbstractOrganizableObject.NAME, "NotExistingMeasure...", FilterType.EQUALS);
		
		engine = ExecutionEngine.builder().withPlugin(new FunctionPlugin()).withPlugin(new ThreadPoolPlugin())
				.withPlugin(new LocalFunctionPlugin()).withPlugin(new BaseArtefactPlugin())
				.withPlugin(new PerformanceAssertPlugin()).build();
	}
	
	@Test
	public void testPositive1() throws IOException {
		PerformanceAssert assert1 = new PerformanceAssert(filterMyMeasure1, Aggregator.COUNT, Comparator.EQUALS, 1l);
		PlanRunnerResult result = execute(assert1);
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testPositive2() throws IOException {
		PerformanceAssert assert1 = new PerformanceAssert(filterMyMeasure1, Aggregator.MAX, Comparator.EQUALS, 1000l);
		PlanRunnerResult result = execute(assert1);
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testPositive3() throws IOException {
		PerformanceAssert assert1 = new PerformanceAssert(filterMyMeasure1, Aggregator.MIN, Comparator.HIGHER_THAN, 100l);
		PlanRunnerResult result = execute(assert1);
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testPositive4() throws IOException {
		PerformanceAssert assert1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 3l, filterMyMeasure2Regex);
		PerformanceAssert assert2 = new PerformanceAssert(Aggregator.AVG, Comparator.EQUALS, 370l, filterMyMeasure2Regex);
		PerformanceAssert assert3 = new PerformanceAssert(Aggregator.MAX, Comparator.EQUALS, 1000l, filterMyMeasure2Regex);
		PerformanceAssert assert4 = new PerformanceAssert(Aggregator.MIN, Comparator.EQUALS, 10l, filterMyMeasure2Regex);
		PerformanceAssert assert5 = new PerformanceAssert(Aggregator.SUM, Comparator.EQUALS, 1110l, filterMyMeasure2Regex);
		PlanRunnerResult result = execute(assert1,assert2,assert3,assert4,assert5);
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testAssertWithinKeywordPositive1() throws IOException {
		PerformanceAssert assert1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 3l, filterMyMeasure2Regex);
		PerformanceAssert assert2 = new PerformanceAssert(Aggregator.AVG, Comparator.EQUALS, 370l, filterMyMeasure2Regex);
		PerformanceAssert assert3 = new PerformanceAssert(Aggregator.MAX, Comparator.EQUALS, 1000l, filterMyMeasure2Regex);
		PerformanceAssert assert4 = new PerformanceAssert(Aggregator.MIN, Comparator.EQUALS, 10l, filterMyMeasure2Regex);
		PerformanceAssert assert5 = new PerformanceAssert(Aggregator.SUM, Comparator.EQUALS, 1110l, filterMyMeasure2Regex);

		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.sequence())
					.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements"))
						.add(assert1).add(assert2).add(assert3).add(assert4).add(assert5)
					.endBlock()
					.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements"))
						.add(assert1).add(assert2).add(assert3).add(assert4).add(assert5)
					.endBlock()
				.endBlock().build();

		PlanRunnerResult result = engine.execute(plan);
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}

	@Test
	public void testAssertWithinKeywordPositiveDynamic() throws IOException {
		//filterMyMeasure2Regex.setField(new DynamicValue<>("paramField",""));
		filterMyMeasure2Regex.setFilter(new DynamicValue<>("paramField",""));
		PerformanceAssert assert1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 3l, filterMyMeasure2Regex);
		Set set = new Set();
		set.setKey(new DynamicValue<String>("paramField"));
		set.setValue(new DynamicValue<String>(".*Measure2*"));
		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.sequence()).add(set)
				.startBlock(BaseArtefacts.sequence())
				.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements"))
				.add(assert1)
				.endBlock()
				.endBlock()
				.endBlock().build();

		PlanRunnerResult result = engine.execute(plan);
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testAssertKeywordMeasureWithinKeywordPositive1() throws IOException {
		PerformanceAssert assert1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 1l, filterKeywordMeasure);
		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.sequence())
					.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements"))
						.add(assert1)
					.endBlock()
					.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements"))
						.add(assert1)
					.endBlock()
				.endBlock().build();
		
		PlanRunnerResult result = engine.execute(plan);
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testAssertWithinThreadGroupPositive1() throws IOException {
		PerformanceAssert assertWithinThreadGroup1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 2l, filterMyMeasure1Equals);
		PerformanceAssert assertWithinKeyword1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 1l, filterMyMeasure1Equals);
		PerformanceAssert assertAfterThreadGroup1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 2l, filterMyMeasure1Equals);
		
		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.sequence())
					.startBlock(BaseArtefacts.threadGroup(2, 1))
						.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements"))
							.add(assertWithinKeyword1)
						.endBlock()
						.add(assertWithinThreadGroup1)
					.endBlock()
					.add(assertAfterThreadGroup1)
				.endBlock()
				.build();

		PlanRunnerResult result = engine.execute(plan);
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testAssertWithinThreadGroupPositive2() throws IOException {
		PerformanceAssert assertWithinThreadGroup1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 1l, filterMyMeasure1Equals);
		PerformanceAssert assertWithinKeyword1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 1l, filterMyMeasure1Equals);
		PerformanceAssert assertAfterThreadGroup1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 1l, filterMyMeasure1Equals);
		
		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.sequence())
					.startBlock(BaseArtefacts.threadGroup(1, 1))
						.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements"))
							.add(assertWithinKeyword1)
						.endBlock()
						.add(assertWithinThreadGroup1)
					.endBlock()
					.add(assertAfterThreadGroup1)
				.endBlock()
				.build();

		PlanRunnerResult result = engine.execute(plan);
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
		PlanRunnerResultAssert.assertEquals("Sequence:PASSED:\r\n"
				+ " ThreadGroup:PASSED:\r\n"
				+ "  Thread 1:PASSED:\r\n"
				+ "   Session:PASSED:\r\n"
				+ "    Iteration 1:PASSED:\r\n"
				+ "     TestKeywordWithMeasurements:PASSED:\r\n"
				+ "      PerformanceAssert:PASSED:\r\n"
				+ "  PerformanceAssert:PASSED:\r\n"
				+ " PerformanceAssert:PASSED:\r\n", result);
	}
	
	/**
	 * Ensure that all the asserts are being executed and are failing. 
	 * Also ensure that the error propagation is working 
	 */
	@Test
	public void testAssertWithinThreadGroupNegative1() throws IOException {
		PerformanceAssert assertWithinThreadGroup1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 2l, filterMyMeasure1Equals);
		PerformanceAssert assertWithinKeyword1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 2l, filterMyMeasure1Equals);
		PerformanceAssert assertAfterThreadGroup1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 2l, filterMyMeasure1Equals);
		
		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.sequence())
					.startBlock(BaseArtefacts.threadGroup(1, 1))
						.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements"))
							.add(assertWithinKeyword1)
						.endBlock()
						.add(assertWithinThreadGroup1)
					.endBlock()
					.add(assertAfterThreadGroup1)
				.endBlock()
				.build();

		PlanRunnerResult result = engine.execute(plan);
		assertEquals(ReportNodeStatus.FAILED, result.getResult());
		PlanRunnerResultAssert.assertEquals("Sequence:FAILED:\r\n"
				+ " ThreadGroup:FAILED:\r\n"
				+ "  Thread 1:FAILED:\r\n"
				+ "   Session:FAILED:\r\n"
				+ "    Iteration 1:FAILED:\r\n"
				+ "     TestKeywordWithMeasurements:FAILED:\r\n"
				+ "      PerformanceAssert:FAILED:Count of myMeasure1 expected to be equal to 2 but was 1\r\n"
				+ "  PerformanceAssert:FAILED:Count of myMeasure1 expected to be equal to 2 but was 1\r\n"
				+ " PerformanceAssert:FAILED:Count of myMeasure1 expected to be equal to 2 but was 1\r\n"
				+ "", result);
	}
	
	@Test
	public void testAssertCombined1() throws IOException {
		PerformanceAssert assertOverall1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 6l, filterMyMeasure2Regex);
		PerformanceAssert assertWithinKeyword1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 3l, filterMyMeasure2Regex);

		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.sequence())
					.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements")).add(assertWithinKeyword1).endBlock()
					.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements")).add(assertWithinKeyword1).endBlock()
					.add(assertOverall1)
				.endBlock()
				.build();

		PlanRunnerResult result = engine.execute(plan);
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testInvalidFilter() throws IOException {
		PerformanceAssert assert1 = new PerformanceAssert(Aggregator.COUNT, Comparator.EQUALS, 1l, invalidFilter);
		Plan plan = PlanBuilder.create()
				.startBlock(BaseArtefacts.sequence())
					.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements"))
						.add(assert1)
					.endBlock()
					.startBlock(FunctionArtefacts.keyword("TestKeywordWithMeasurements"))
						.add(assert1)
					.endBlock()
				.endBlock().build();
		
		PlanRunnerResult result = engine.execute(plan);
		// This should fail as not measure corresponding to the specified filter could be found
		assertEquals(ReportNodeStatus.FAILED, result.getResult());
	}
	
	@Test
	public void testNegative1() throws IOException {
		PerformanceAssert assert1 = new PerformanceAssert(filterMyMeasure1, Aggregator.MAX, Comparator.LOWER_THAN, 100l);
		PlanRunnerResult result = execute(assert1);
		PlanRunnerResultAssert.assertEquals("Sequence:FAILED:\r\n"
				+ " TestKeywordWithMeasurements:PASSED:\r\n"
				+ " PerformanceAssert:FAILED:Max of myMeasure1 expected to be lower than 100 but was 1000\r\n"
				+ "", result);
	}
	
	@Test
	public void testNegative2() throws IOException {
		PerformanceAssert assert1 = new PerformanceAssert(filterMyMeasure1, Aggregator.MIN, Comparator.LOWER_THAN, 1000l);
		PlanRunnerResult result = execute(assert1);
		PlanRunnerResultAssert.assertEquals("Sequence:FAILED:\r\n"
				+ " TestKeywordWithMeasurements:PASSED:\r\n"
				+ " PerformanceAssert:FAILED:Min of myMeasure1 expected to be lower than 1000 but was 1000\r\n"
				+ "", result);
	}
	

	@Test
	public void testNegative3() throws IOException {
		PerformanceAssert assert1 = new PerformanceAssert(filterMyMeasure1, Aggregator.AVG, Comparator.HIGHER_THAN, 500l);
		PlanRunnerResult result = execute(assert1);
		PlanRunnerResultAssert.assertEquals("Sequence:PASSED:\r\n"
				+ " TestKeywordWithMeasurements:PASSED:\r\n"
				+ " PerformanceAssert:PASSED:\r\n"
				+ "", result);
	}
	
	@Test
	public void testNegative4() throws IOException {
		PerformanceAssert assert1 = new PerformanceAssert(filterMyMeasure1, Aggregator.AVG, Comparator.LOWER_THAN, 500l);
		PlanRunnerResult result = execute(assert1);
		PlanRunnerResultAssert.assertEquals("Sequence:FAILED:\r\n"
				+ " TestKeywordWithMeasurements:PASSED:\r\n"
				+ " PerformanceAssert:FAILED:Average of myMeasure1 expected to be lower than 500 but was 1000\r\n"
				+ "", result);
	}

	protected PlanRunnerResult execute(PerformanceAssert... asserts) {
		Plan plan = plan(asserts);
		PlanRunnerResult result = engine.execute(plan);
		return result;
	}
	
	protected Plan plan(PerformanceAssert... asserts) {
		PlanBuilder planBuilder = PlanBuilder.create().startBlock(BaseArtefacts.sequence())
				.add(FunctionArtefacts.keyword("TestKeywordWithMeasurements"));
		Arrays.asList(asserts).forEach(planBuilder::add);
		return planBuilder.endBlock().build();
	}

	@Keyword
	public void TestKeywordWithMeasurements() {
		output.addMeasure("myMeasure1", 1000);
		output.addMeasure("myMeasure2", 1000);
		output.addMeasure("myMeasure2", 100);
		output.addMeasure("myMeasure2", 10);
	}

}
