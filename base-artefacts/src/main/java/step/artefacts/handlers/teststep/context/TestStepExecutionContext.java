package step.artefacts.handlers.teststep.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import step.adapters.commons.helper.DocumentParser;
import step.adapters.commons.helper.DocumentTransformer;
import step.adapters.commons.model.Input;
import step.adapters.commons.model.Output;
import step.artefacts.reports.TestStepReportNode;
import step.attachments.AttachmentMeta;
import step.core.execution.ExecutionContext;
import step.expressions.ExpressionHandler;
import step.expressions.placeholder.PlaceHolderHandler;
import step.grid.AdapterTokenWrapper;
import step.plugins.keywordrepository.Keyword;

public class TestStepExecutionContext {
	
	private DocumentParser parser = new DocumentParser();
	
	private DocumentTransformer transformer = new DocumentTransformer();
	
	private Document inputDocument;
	
	private Input input;
	
	private Output output;
	
	private Map<String,String> outputAsMap;
	
	private boolean skipped = false;
	
	private Keyword keyword;
	
	private List<AttachmentMeta> attachments = new ArrayList<>();
	
	private List<Message> exceptions = new ArrayList<>();
	
	private AdapterTokenWrapper adapterToken;
	
	private ExpressionHandler expressionHandler;
	
	private TestStepReportNode node;
	
	public class Error {
		String message;
		
		Exception e;
	}

	public TestStepExecutionContext(TestStepReportNode node) {
		super();
		this.node = node;
		
		ExecutionContext context = ExecutionContext.getCurrentContext();
		HashMap<String,String> outputParameterMap = new HashMap<String,String>();			
		ExpressionHandler expressionHandler = new ExpressionHandler(new PlaceHolderHandler(context, outputParameterMap));
		setExpressionHandler(expressionHandler);
	}

	public Document getInputDocument() {
		return inputDocument;
	}

	public void setInputDocument(Document inputDocument) {
		this.inputDocument = inputDocument;
	}

	public Input getInput() {
		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}

	public Output getOutput() {
		return output;
	}

	public void setOutput(Output output) {
		this.output = output;
	}
	
	public Map<String, String> getOutputAsMap() {
		return outputAsMap;
	}

	public void setOutputAsMap(Map<String, String> outputAsMap) {
		this.outputAsMap = outputAsMap;
	}

	public boolean hasError() {
		return exceptions.size()>0;
	}

	public boolean hasBusinessError() {
		for(Message e:exceptions) {
			if(e instanceof BusinessError) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasTechnicalError() {
		for(Message e:exceptions) {
			if(!(e instanceof BusinessError)) {
				return true;
			}
		}
		return false;
	}

	public boolean isSkipped() {
		return skipped;
	}

	public void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}
	
	public void addAttachment(AttachmentMeta attachment) {
		attachments.add(attachment);
	}
	
	public List<AttachmentMeta> getAttachments() {
		return attachments;
	}

	public void addMessage(Message exception) {
		exceptions.add(exception);
	}

	public List<Message> getExceptions() {
		return exceptions;
	}

	public AdapterTokenWrapper getAdapterToken() {
		return adapterToken;
	}

	public void setAdapterToken(AdapterTokenWrapper adapterToken) {
		this.adapterToken = adapterToken;
	}

	public DocumentParser getParser() {
		return parser;
	}

	public void setParser(DocumentParser parser) {
		this.parser = parser;
	}

	public DocumentTransformer getTransformer() {
		return transformer;
	}

	public void setTransformer(DocumentTransformer transformer) {
		this.transformer = transformer;
	}

	public ExpressionHandler getExpressionHandler() {
		return expressionHandler;
	}

	public void setExpressionHandler(ExpressionHandler expressionHandler) {
		this.expressionHandler = expressionHandler;
	}

	public TestStepReportNode getNode() {
		return node;
	}

	public void setNode(TestStepReportNode node) {
		this.node = node;
	}

	public Keyword getKeyword() {
		return keyword;
	}

	public void setKeyword(Keyword keyword) {
		this.keyword = keyword;
	}
}
