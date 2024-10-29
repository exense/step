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
package step.reporting;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xmlunit.assertj.XmlAssert;
import step.artefacts.*;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plans.runner.PlanRunnerResultAssert;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.datapool.sequence.IntSequenceDataPool;
import step.threadpool.ThreadPoolPlugin;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.file.Files;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class JUnit4ReportWriterTest {

	private static final Logger log = LoggerFactory.getLogger(JUnit4ReportWriterTest.class);

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

		try (ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).withPlugin(new TokenForecastingExecutionPlugin()).build()) {
			engine.execute(plan).writeReport(new JUnit4ReportWriterTestable(), report);
		}

		log.info("Generated report:");
		String generatedReport = new String(Files.readAllBytes(report.toPath()));
		log.info("\n" + generatedReport);
		validateWithXsd(report);

		XmlAssert.assertThat(generatedReport)
				.and(PlanRunnerResultAssert.readResource(this.getClass(), "TEST-JUnit4ReportWriterTest-testTestset-expected.xml"))
				.areSimilar();
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

		try (ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new BaseArtefactPlugin()).withPlugin(new TokenForecastingExecutionPlugin()).build()) {
			engine.execute(plan).writeReport(new JUnit4ReportWriterTestable(), report);
		}

		log.info("Generated report:");
		String generatedReport = new String(Files.readAllBytes(report.toPath()));
		log.info("\n" + generatedReport);
		validateWithXsd(report);

		XmlAssert.assertThat(generatedReport)
				.and(PlanRunnerResultAssert.readResource(this.getClass(), "TEST-JUnit4ReportWriterTest-testSimpleSequence-expected.xml"))
				.areSimilar();

	}

	@Test
	public void testSeveralTestSuites() throws IOException {
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

		File report = new File("TEST-JUnit4ReportWriterTest-testMultiTestsuites.xml");
		report.deleteOnExit();

		// execute plan twice
		try (FileOutputStream fos = new FileOutputStream(report); OutputStreamWriter writer = new OutputStreamWriter(fos)) {

			try (ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new BaseArtefactPlugin()).withPlugin(new TokenForecastingExecutionPlugin()).build()) {
				PlanRunnerResult result1 = engine.execute(plan);
				PlanRunnerResult result2 = engine.execute(plan);
				new JUnit4ReportWriterTestable().writeMultiReport(
						engine.getExecutionEngineContext().getReportNodeAccessor(),
						List.of(result1.getExecutionId(), result2.getExecutionId()),
						writer
				);
			}

			log.info("Generated report:");
			String generatedReport = new String(Files.readAllBytes(report.toPath()));
			log.info("\n" + generatedReport);
			validateWithXsd(report);

			XmlAssert.assertThat(generatedReport)
					.and(PlanRunnerResultAssert.readResource(this.getClass(), "TEST-JUnit4ReportWriterTest-testSeveralTestsuites-expected.xml"))
					.areSimilar();
		}
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

	private Validator initValidator() throws SAXException, IOException {
		String xsdPath = "src/test/resources/junitReport/JUnit.xsd";
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try(FileInputStream fis = new FileInputStream(xsdPath)) {
			Source schemaFile = new StreamSource(fis);
			Schema schema = factory.newSchema(schemaFile);
			return schema.newValidator();
		}
    }

	private void validateWithXsd(File report) throws IOException {
		try {
			Validator xsdValidator = initValidator();
			xsdValidator.validate(new StreamSource(report));
		} catch (SAXException ex) {
			log.error("Xml report is invalid", ex);
			Assert.fail("XSD validation exception");
		}
	}

	private static class JUnit4ReportWriterTestable extends JUnit4ReportWriter {
		@Override
		protected long getExecutionTime(AtomicLong executionTime) {
			return 1730042979873L;
		}

		@Override
		protected ZoneId getZoneId() {
			return ZoneId.of("UTC");
		}

		@Override
		protected String getHostName() {
			return "localhost";
		}

		@Override
		protected long getTestSuiteDuration(AtomicLong duration) {
			return 69;
		}

		@Override
		protected Integer getTestCaseDuration(ReportNode node) {
			return 31;
		}
	}

	
}
