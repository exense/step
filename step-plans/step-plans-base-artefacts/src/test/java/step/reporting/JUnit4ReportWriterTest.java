package step.reporting;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import step.artefacts.BaseArtefactPlugin;
import step.artefacts.ForBlock;
import step.artefacts.Sequence;
import step.artefacts.TestCase;
import step.artefacts.TestSet;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResultAssert;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.datapool.sequence.IntSequenceDataPool;
import step.threadpool.ThreadPoolPlugin;

public class JUnit4ReportWriterTest {

	@Test
	public void testTestset() throws IOException {
		Plan plan = PlanBuilder.create()
				.startBlock(testSet("TS01"))
					.startBlock(testCase("TC01 - PASSED"))
						.add(checkArtefact(ReportNodeStatus.PASSED))
					.endBlock()
					.startBlock(testCase("TC02 - PASSED in Sequence"))
						.startBlock(sequence())
							.add(checkArtefact(ReportNodeStatus.PASSED))
							.add(checkArtefact(ReportNodeStatus.PASSED))
						.endBlock()
					.endBlock()
					.startBlock(testCase("TC03 - FAILED without error message"))
						.add(checkArtefact(ReportNodeStatus.PASSED))
						.add(checkArtefact(ReportNodeStatus.FAILED))
					.endBlock()
					.startBlock(testCase("TC04 - FAILED with error message"))
						.startBlock(for_())
							.add(checkArtefact(ReportNodeStatus.FAILED, "my message"))
						.endBlock()
					.endBlock()
					.startBlock(testCase("TC05 - TECH_ERROR"))
						.add(checkArtefact(ReportNodeStatus.TECHNICAL_ERROR))
					.endBlock()
					.startBlock(testCase("TC06 - TECH_ERROR with message"))
						.add(checkArtefact(ReportNodeStatus.TECHNICAL_ERROR, "My error message"))
					.endBlock()
					.startBlock(testCase("TC07 - SKIPPED"))
						.add(checkArtefact(ReportNodeStatus.SKIPPED))
					.endBlock()
					.startBlock(testCase("TC08 - NORUN"))
						.add(checkArtefact(ReportNodeStatus.NORUN))
					.endBlock()
				.endBlock().build();
		
		File report = new File("TEST-JUnit4ReportWriterTest-testTestset.xml");
		report.deleteOnExit();
		
		ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).build();
		engine.execute(plan).writeReport(new JUnit4ReportWriter(), report);
		
		PlanRunnerResultAssert.assertEquals(this.getClass(), "TEST-JUnit4ReportWriterTest-testTestset-expected.xml", report, "time=\".+?\"");
	}
	
	@Test
	public void testSimpleSequence() throws IOException {
		Plan plan = PlanBuilder.create()
				.startBlock(sequence())
					.add(checkArtefact(ReportNodeStatus.PASSED))
					.startBlock(sequence())
						.add(checkArtefact(ReportNodeStatus.PASSED))
						.add(checkArtefact(ReportNodeStatus.PASSED))
					.endBlock()
					.add(checkArtefact(ReportNodeStatus.FAILED))
					.add(checkArtefact(ReportNodeStatus.FAILED, "my message"))
					.add(checkArtefact(ReportNodeStatus.TECHNICAL_ERROR))
					.startBlock(testCase("TC - TECH_ERROR with message"))
						.add(checkArtefact(ReportNodeStatus.TECHNICAL_ERROR, "My error message"))
					.endBlock()
					.add(checkArtefact(ReportNodeStatus.SKIPPED))
					.add(checkArtefact(ReportNodeStatus.NORUN))
					.add(checkArtefact(ReportNodeStatus.INTERRUPTED))
				.endBlock().build();
		
		File report = new File("TEST-JUnit4ReportWriterTest-testSimpleSequence.xml");
		report.deleteOnExit();
		
		ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new BaseArtefactPlugin()).build();
		engine.execute(plan).writeReport(new JUnit4ReportWriter(), report);
		
		PlanRunnerResultAssert.assertEquals(this.getClass(), "TEST-JUnit4ReportWriterTest-testSimpleSequence-expected.xml", report, "time=\".+?\"");
	}
	
	protected Sequence sequence() {
		Sequence sequence = new Sequence();
		sequence.getContinueOnError().setValue(true);
		return sequence;
	}

	protected TestSet testSet(String name) {
		TestSet testSet = new TestSet();
		testSet.getAttributes().put("name", name);
		return testSet;
	}
	
	protected TestCase testCase(String name) {
		TestCase testCase = new TestCase();
		testCase.getAttributes().put("name", name);
		return testCase;
	}
	
	protected ForBlock for_() {
		ForBlock forBlock = new ForBlock();
		IntSequenceDataPool dataSource = new IntSequenceDataPool();
		forBlock.setDataSource(dataSource);
		return forBlock;
	}
	
	protected CheckArtefact checkArtefact(final ReportNodeStatus status) {
		return new CheckArtefact(c->c.getCurrentReportNode().setStatus(status));
	}
	
	protected CheckArtefact checkArtefact(final ReportNodeStatus status, String error) {
		return new CheckArtefact(c->{
			c.getCurrentReportNode().setStatus(status);
			c.getCurrentReportNode().setError(new Error(status==ReportNodeStatus.TECHNICAL_ERROR?ErrorType.TECHNICAL:ErrorType.BUSINESS, error));
		});
	}
	
}
