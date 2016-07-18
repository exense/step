package step.artefacts.handlers.teststep;

import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import step.adapters.commons.model.Input;
import step.adapters.commons.model.InputBuilder;
import step.artefacts.handlers.teststep.context.TestStepExecutionContext;
import step.core.execution.ExecutionContext;
import step.expressions.ExpressionHandler;
import step.expressions.placeholder.PlaceHolderHandler;

public class TestStepInputBuilder {

	private final TestStepExecutionContext stepExecutionContext;

	public TestStepInputBuilder(TestStepExecutionContext stepExecutionContext) {
		super();
		this.stepExecutionContext = stepExecutionContext;
	}

	public void buildTestStepInput() {	
		Document document = stepExecutionContext.getInputDocument();
		if(document!=null) {
			Document documentAfterReplacements = transformDocument(document);
			buildInput(documentAfterReplacements);
		
			transformDocumentAndBuildInput(document);
		}
	}
	
	private Input transformDocumentAndBuildInput(Document document) {				
		ExpressionHandler expressionHandler = new ExpressionHandler(new PlaceHolderHandler(ExecutionContext.getCurrentContext(), new HashMap<String, String>()));
		Document documentAfterReplacements = expressionHandler.handleInput(document);
		
		Input input = buildInput(documentAfterReplacements);
		stepExecutionContext.setInput(input);
		return input;
	}
	
	public Document transformDocument(Document document) {				
		ExpressionHandler expressionHandler = new ExpressionHandler(new PlaceHolderHandler(ExecutionContext.getCurrentContext(), new HashMap<String, String>()));
		Document documentAfterReplacements = expressionHandler.handleInput(document);
		
		return documentAfterReplacements;
	}
	
	
	private Input buildInput(Document payload) {
		InputBuilder inputBuilder = new InputBuilder();
		inputBuilder.setPayload(payload);

		setInputParameters(inputBuilder);
		return inputBuilder.build();
	}
	
	private void setInputParameters(InputBuilder inputBuilder) {
		ExecutionContext context = ExecutionContext.getCurrentContext();
		inputBuilder.setUserID(context.getExecutionParameters().getUserID());
		
		for(Entry<String, Object> entry:context.getVariablesManager().getAllVariables().entrySet()) {
			inputBuilder.addParameter(entry.getKey(), entry.getValue().toString());
		}
	}
}
