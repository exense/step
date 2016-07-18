package step.artefacts.handlers.teststep;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import step.adapters.commons.model.ParserException;
import step.artefacts.Entry;
import step.artefacts.TestStep;
import step.artefacts.handlers.CheckHandler;
import step.artefacts.handlers.SetHandler;
import step.artefacts.handlers.teststep.context.TestStepExecutionContext;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.ValidationException;

public class ExpectedBlockExecutor {

	private final TestStepExecutionContext stepExecutionContext;
	
	public ExpectedBlockExecutor(TestStepExecutionContext stepExecutionContext) {
		super();
		this.stepExecutionContext = stepExecutionContext;
	}

	public void executeExpectedBlock(TestStep testArtefact) {
		if(!stepExecutionContext.isSkipped()) {
			Expected expected = parseExpected(testArtefact);	

			if(expected!=null) {	
				ExecutionContext context = ExecutionContext.getCurrentContext();
				if(expected.getOnFailure()!=null) {
					boolean continueOnError = expected.getOnFailure().equals("continue");
					context.getVariablesManager().putVariable(context.getReportNodeTree().getParent(stepExecutionContext.getNode()), ArtefactHandler.CONTINUE_EXECUTION_ONCE, continueOnError);					
				}				
				
				// Sets are always executed, even if a TECHNICAL_ERRROR occurred 
				executeSets(expected);	
			
				if(!stepExecutionContext.hasError() && !context.isSimulation()) {		
					executeChecks(expected);
				}
			}
		}
	}
	
	private Expected parseExpected(TestStep testArtefact) {
		String expectedOutput = testArtefact.getExpectedOutput();
		if(expectedOutput!=null && expectedOutput.trim().length()>0) {
			Document expectedDocument;
			try {
				expectedDocument = stepExecutionContext.getParser().parse(expectedOutput);
			} catch (ParserException e) {
				throw new ValidationException(e.getMessage());
			}
			return buildExpected(expectedDocument);
		} else {
			return null;
		}
	}
	
	public static Expected buildExpected(Document document) {
		Expected expected = new Expected();
		
		Element root = document.getDocumentElement();

		for (int i = 0; i < root.getAttributes().getLength(); i++) {
			if (root.getAttributes().item(i).getNodeType() == Node.ATTRIBUTE_NODE) {
				String name = ((Node)root.getAttributes().item(i)).getNodeName();
				String value = ((Node)root.getAttributes().item(i)).getNodeValue();
				
				if(name.startsWith("check")) {
					expected.getChecksExpressions().put(name, value);
				} else if(name.startsWith("set")) {
					expected.getSetExpressions().add(new Entry(name, value));
				} else if(name.equals("onFailure")) {
					expected.setOnFailure(value);
				}
			}
		}
		
		return expected;
	}
	
	private void executeChecks(Expected expected) {
		CheckHandler.executeChecks(stepExecutionContext, expected.getChecksExpressions());
	}

	private void executeSets(Expected expected) {
		SetHandler.executeSets(stepExecutionContext, expected.getSetExpressions());
	}

}
