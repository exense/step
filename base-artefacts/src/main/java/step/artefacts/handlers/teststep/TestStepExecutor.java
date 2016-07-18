package step.artefacts.handlers.teststep;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import step.adapters.commons.model.AdapterMessageBuilder;
import step.adapters.commons.model.AttachmentHelper;
import step.adapters.commons.model.Input;
import step.adapters.commons.model.Output;
import step.adapters.commons.model.OutputBuilder;
import step.adapters.commons.model.ParserException;
import step.artefacts.handlers.SessionHandler;
import step.artefacts.handlers.teststep.context.BusinessError;
import step.artefacts.handlers.teststep.context.TechnicalError;
import step.artefacts.handlers.teststep.context.TestStepExecutionContext;
import step.artefacts.handlers.teststep.context.Warning;
import step.attachments.AttachmentMeta;
import step.commons.conf.Configuration;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.miscellaneous.ReportNodeAttachmentManager.AttachmentQuotaException;
import step.expressions.ExpressionHandler;
import step.expressions.XmlToParameterMapTranslator;
import step.expressions.placeholder.PlaceHolderHandler;
import step.grid.client.AdapterClient;
import step.grid.client.AdapterSession;
import step.grid.client.AdapterClient.ProcessInputResponse;
import step.plugins.adaptergrid.AdapterClientPlugin;
import step.plugins.keywordrepository.Keyword;
import step.plugins.keywordrepository.KeywordRepository;
import step.plugins.keywordrepository.KeywordType;

public class TestStepExecutor {
	
	private static Logger logger = LoggerFactory.getLogger(TestStepExecutor.class);
	
	public final TestStepExecutionContext stepExecutionContext;
	
	public TestStepExecutor(TestStepExecutionContext stepExecutionContext) {
		super();
		this.stepExecutionContext = stepExecutionContext;
	}

	public void executeStep() {
		Input input = stepExecutionContext.getInput();
		if(input!=null) {
			Keyword keyword = getKeywordConfiguration(input);
			stepExecutionContext.setKeyword(keyword);
			
			if(!keyword.hasSchema() || validateSchema(input.getPayload(), input.getKeyword())) {
				callAdapterAndProcessOutput();
			}
		}
	}

	public void callAdapterAndProcessOutput() {
		Input input = stepExecutionContext.getInput();
		
		boolean isInputSkipped = isSkipped(input); 
		stepExecutionContext.setSkipped(isInputSkipped);
		
		ExecutionContext context = ExecutionContext.getCurrentContext();
		if(!isInputSkipped) {	
			Output output = null;
			if (context.isSimulation()) {
				output = buildDummyOutput(input);
			} else {
				try {
					output = callAdapter();
				} catch (Exception e) {
					stepExecutionContext.addMessage(new TechnicalError("Error while calling adapter: " + e.getMessage(),e));
				}
			}
			
			stepExecutionContext.setOutput(output);
			
			XmlToParameterMapTranslator translator = new XmlToParameterMapTranslator();
			HashMap<String,String> outputParameterMap = output!=null&&output.getPayload()!=null?translator.getParameterMap(output.getPayload()):new HashMap<String,String>();			
			ExpressionHandler expressionHandler = new ExpressionHandler(new PlaceHolderHandler(context, outputParameterMap));
			stepExecutionContext.setOutputAsMap(outputParameterMap);
			stepExecutionContext.setExpressionHandler(expressionHandler);

			context.getVariablesManager().putVariable(stepExecutionContext.getNode(), "output", output);
			//context.getVariablesManager().putVariable(node, "outputXML", (new XmlSlurper()).parseText(output.getContent()));
			
			processOutputAttachments();
								
			if(output!=null) {
				if(output.getTechnicalError()==null) {
					if(!output.hasBusinessError()) {
						validateOutput(output, input.getKeyword());
					} else {
						stepExecutionContext.addMessage(new BusinessError(output.getBusinessError()));
					}
				} else {
					stepExecutionContext.addMessage(new TechnicalError("Adapter error: " + output.getTechnicalError()));
				}
			} else {
				stepExecutionContext.addMessage(new TechnicalError("Adapter error: The adapter returned no output."));
			}
		} else {
			logger.debug(context.getExecutionId() + ". Step skipped.");
		}
	}
	
	private Schema getSchema(String keyword) {
		SchemaFactory schameFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		Schema schema;
		
		try {
			File schemaFile = getSchemaFile(keyword);
			schema = schameFactory.newSchema(new StreamSource(schemaFile));
		} catch (SAXException e) {
			throw new RuntimeException("Unable to create schema.",e);
		}
		return schema;
	}
	
	private File getSchemaFile(String keyword) {
		String schemaLocation = ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsString("tec.schemalocation");
		String schemaBaseDir = Configuration.getInstance().getProperty("tec.schemadir");
		File schemaFile = new File(schemaBaseDir+ "/" + schemaLocation + "/" + keyword + ".xsd");
		return schemaFile;
	}
	
	private void validateOutput(Output output, String keyword) {
		Document document = output.getPayload();
		if(document!=null) {
			if(stepExecutionContext.getKeyword().hasSchema()) {
				validateSchema(document, keyword);
			}
		} else {
			stepExecutionContext.addMessage(new TechnicalError("The adapter returned an empty Output and no error message."));
		}
	}
	
	private boolean validateSchema(Document document, String keyword) {
		if(document.getDocumentElement().getNamespaceURI()==null) {
			document.renameNode(document.getDocumentElement(), "http://step.org/suva/"+keyword,document.getDocumentElement().getNodeName());
		}

		Schema schema = getSchema(keyword);
		Validator validator = schema.newValidator();

		try {
			validator.validate(new DOMSource(document));
			return true;
		} catch (SAXParseException e) {
			stepExecutionContext.addMessage(new TechnicalError(e.getMessage()));
			return false;
		} catch (SAXException | IOException e) {
			throw new RuntimeException(e);
		}
		
	}



	private Output callAdapter() throws Exception {
		Input input = stepExecutionContext.getInput();
		Output output;
		
		// Define permitId here
		UUID permitId = UUID.randomUUID();
				
		try {
			Keyword kwConf = stepExecutionContext.getKeyword();
			
			if(KeywordType.LOCAL.equals(kwConf.getType())) {
				output = callLocalAdapter(permitId, input);
			} else {
				output = callRemoteAdapter(permitId, kwConf);						
			}
		} finally {
			// release quota and capacity permits in return AdapterClient->#returnAdapterTokenToRegister
		}
		return output;
	}

	private Output callRemoteAdapter(UUID permitId, Keyword kwConf) throws Exception {
		Input input = stepExecutionContext.getInput();
		
		input.
		
		AdapterClient client = (AdapterClient) ExecutionContext.getCurrentContext().getGlobalContext().get(AdapterClientPlugin.ADAPTER_CLIENT_KEY);
		ProcessInputResponse response = client.processInput(agetAdapterSession(), kwConf, input, permitId);

		stepExecutionContext.setAdapterToken(response.getToken());
		return response.getOutput();
	
	
	private AdapterSession agetAdapterSession() {
		Object o = ExecutionContext.getCurrentContext().getVariablesManager().getVariable(SessionHandler.ADAPTER_SESSION_PARAM_KEY);
		if(o!=null && o instanceof AdapterSession) {
			return (AdapterSession) o;
		} else {
			return null;
		}
	}

	private Output buildDummyOutput(Input input) {
		AdapterMessageBuilder<Output> outputBuilder = new OutputBuilder();
		String dummyContent = SchemaInstanceGeneratorBridge.xsd2inst(new File[] { getSchemaFile(input.getKeyword()) }, "Return");
		try {
			outputBuilder.setPayload(dummyContent);
		} catch (ParserException e) {
			throw new RuntimeException(e);
		}
		return outputBuilder.build();
	}
	
	private Keyword getKeywordConfiguration(Input input) {
		KeywordRepository keywordRepository = (KeywordRepository) ExecutionContext.getCurrentContext().getGlobalContext().get(AdapterClientPlugin.KEYWORD_REPOSITORY_KEY);
		Keyword keywordConf = keywordRepository.getConfigurationForKeyword(ExecutionContext.getCurrentContext(), input.getKeyword());
		
		if(keywordConf==null) {
			throw new RuntimeException("No configuration found for keyword " + input.getKeyword()
					+ ". Please ensure that the keyword is properly defined in the ConfigurationRepository.");
		} else {
			return keywordConf;
		}
	}
	
	private Output callLocalAdapter(UUID permitId, Input input) throws Exception {
		
		/*
		 * Do not acquire permit for local adapters here!
		 * 
		 * Could end in a deadlock situation waiting for permit 
		 * when an other execution in the same thread has a permit and 
		 * waits for adapter token
		 */
		// acquirePermit(permitId, input);
		
		String adapterClassname = input.getKeyword() + "Adapter";
		Class<?> adapterClass;
		try {
			adapterClass = Class.forName("step.localadapters."+adapterClassname);
			AbstractLocalAdapter adapter = (AbstractLocalAdapter) adapterClass.newInstance();
			adapter.init();
			return adapter.execute(input);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Unable to create local adapter " + adapterClassname + 
					". Please check the keyword mapping and ensure that the class " + adapterClassname + " exists.", e);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("An error occurred while instantiating local adapter " + adapterClassname, e);
		}
		
	}
	
	private static final String SKIP_STRING = "SKIP";
	
	private boolean isSkipped(Input input) {
		if(ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsBoolean("tec.handlers.step.useskips")) {
			Map<String,String> inputAttr = input.getPayloadAttributes();
			for(String key:inputAttr.keySet()) {
				if(SKIP_STRING.equals(inputAttr.get(key))) {
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}
	
	private void processOutputAttachments() {
		Output output = stepExecutionContext.getOutput();
		if (output!=null && output.getAttachments()!=null) {
			for (step.adapters.commons.model.Attachment attachment : output.getAttachments()) {
				try {
					AttachmentMeta attachmentMeta = ReportNodeAttachmentManager.createAttachment(AttachmentHelper.hexStringToByteArray(attachment.getHexContent()), attachment.getName());
					stepExecutionContext.addAttachment(attachmentMeta);
				} catch (AttachmentQuotaException e) {
					stepExecutionContext.addMessage(new Warning(e.getMessage()));
				}
			}
		}
	}

}
