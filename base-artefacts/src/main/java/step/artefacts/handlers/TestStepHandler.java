package step.artefacts.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import step.adapters.commons.model.ParserException;
import step.artefacts.TestStep;
import step.artefacts.handlers.teststep.ExpectedBlockExecutor;
import step.artefacts.handlers.teststep.TestStepExecutor;
import step.artefacts.handlers.teststep.TestStepInputBuilder;
import step.artefacts.handlers.teststep.TestStepReportNodeBuilder;
import step.artefacts.handlers.teststep.TestStepTransactionManager;
import step.artefacts.handlers.teststep.context.TechnicalError;
import step.artefacts.handlers.teststep.context.TestStepExecutionContext;
import step.artefacts.reports.TestStepReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;

public class TestStepHandler extends ArtefactHandler<TestStep, TestStepReportNode> {

	private static Logger logger = LoggerFactory.getLogger(TestStepHandler.class);
	
	public TestStepHandler() {
		
	}

	@Override
	public void createReportSkeleton_(TestStepReportNode node, TestStep testArtefact) {
		node.setInput(testArtefact.getInput());
	}

	@Override
	public void execute_(final TestStepReportNode node, TestStep testArtefact) {					
		TestStepExecutionContext stepExecutionContext = new TestStepExecutionContext(node);
		
		parseInput(testArtefact, stepExecutionContext);
		
		TestStepInputBuilder inputBuilder = new TestStepInputBuilder(stepExecutionContext);
		inputBuilder.buildTestStepInput();
		
		TestStepExecutor executor = new TestStepExecutor(stepExecutionContext);
		executor.executeStep();
		
		TestStepReportNodeBuilder.updateTestStepReportNode(stepExecutionContext, node);
		
		ExpectedBlockExecutor expectedBlockExecutor = new ExpectedBlockExecutor(stepExecutionContext);
		expectedBlockExecutor.executeExpectedBlock(testArtefact);
		
		TestStepReportNodeBuilder.updateTestStepReportNode(stepExecutionContext, node);
						
		TestStepTransactionManager.processOutputTransactions(stepExecutionContext.getOutput(), node);
	}

	private void parseInput(TestStep testArtefact, TestStepExecutionContext stepExecutionContext) {
		try {
			Document document = stepExecutionContext.getParser().parse(testArtefact.getInput());
			stepExecutionContext.setInputDocument(document);
		} catch (ParserException e) {
			stepExecutionContext.addMessage(new TechnicalError("Error parsing input.",e));
		}
	}

	@Override
	public TestStepReportNode createReportNode_(ReportNode parentNode, TestStep testArtefact) {
		TestStepReportNode report = new TestStepReportNode();
		report.setExpected(testArtefact.getExpectedOutput());
		return report;
	}
}
