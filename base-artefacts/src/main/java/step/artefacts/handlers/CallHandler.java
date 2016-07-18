package step.artefacts.handlers;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import step.adapters.commons.model.ParserException;
import step.artefacts.Call;
import step.artefacts.Sequence;
import step.artefacts.handlers.teststep.SchemaInstanceGeneratorBridge;
import step.artefacts.handlers.teststep.TestStepExecutor;
import step.artefacts.handlers.teststep.TestStepInputBuilder;
import step.artefacts.handlers.teststep.TestStepReportNodeBuilder;
import step.artefacts.handlers.teststep.TestStepTransactionManager;
import step.artefacts.handlers.teststep.context.TechnicalError;
import step.artefacts.handlers.teststep.context.TestStepExecutionContext;
import step.artefacts.reports.TestStepReportNode;
import step.attachments.AttachmentMeta;
import step.commons.conf.Configuration;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.ValidationException;
import step.core.variables.UndefinedVariableException;
import step.core.variables.VariablesManager;
// 
public class CallHandler extends ArtefactHandler<Call, TestStepReportNode> {

	public static final String STEP_NODE_KEY = "currentStep";
	
	public CallHandler() {
		super();
		
		// TODO Auto-generated constructor stub
	}
	
	
	@Override
	protected void createReportSkeleton_(TestStepReportNode parentNode,	Call testArtefact) {
	}
	
	
	@Override
	protected void execute_(TestStepReportNode node, Call testArtefact) {
		TestStepExecutionContext stepExecutionContext = new TestStepExecutionContext(node);
		
		context.getVariablesManager().putVariable(node, CheckHandler.STEP_CONTEXT_PARAM_KEY, stepExecutionContext);
		context.getVariablesManager().putVariable(node, STEP_NODE_KEY, node);
		
		generateInputDocument(testArtefact, stepExecutionContext);
		
		TestStepInputBuilder testSTepInputBuilder = new TestStepInputBuilder(stepExecutionContext);
		testSTepInputBuilder.buildTestStepInput();
		
		TestStepExecutor executor = new TestStepExecutor(stepExecutionContext);
		executor.executeStep();
		
		TestStepReportNodeBuilder.updateTestStepReportNode(stepExecutionContext, node);
		
		if(!stepExecutionContext.isSkipped()) {
			copyOutputToResultMap(testArtefact, stepExecutionContext);
			
			ArtefactAccessor artefactAccessor = context.getGlobalContext().getArtefactAccessor();
			Sequence postExecutionArtefact = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "PostExecutionArtefact");
			for(AbstractArtefact child:getChildren(testArtefact)) {
				postExecutionArtefact.addChild(child.getId());
			}
			
			ReportNode postExecutionReportNode = delegateExecute(postExecutionArtefact, node);
			
			if(postExecutionReportNode.getAttachments()!=null) {
				for(AttachmentMeta attachment:postExecutionReportNode.getAttachments()) {
					stepExecutionContext.addAttachment(attachment);
				}
			}
	
			TestStepReportNodeBuilder.updateTestStepReportNode(stepExecutionContext, node);
			
			TestStepTransactionManager.processOutputTransactions(stepExecutionContext.getOutput(), node);
		}
	}


	private void copyOutputToResultMap(Call testArtefact,
			TestStepExecutionContext stepExecutionContext) {
		String resultMapVarName = testArtefact.getResultMap();
		if(resultMapVarName!=null) {
			Object resultMapVar = context.getVariablesManager().getVariable(resultMapVarName);
			if(resultMapVar!=null && resultMapVar instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, String> resultMap = (Map<String, String>)resultMapVar;
				Map<String, String> outputAsMap = stepExecutionContext.getOutputAsMap();
				if(outputAsMap!=null) { 
					for(String key:outputAsMap.keySet()) {
						try {
							resultMap.put(key, outputAsMap.get(key));
						} catch (ValidationException e) {
							// the entry might not be settable in the map
						}
					}
				}
			}
		}
	}


	private void generateInputDocument(Call testArtefact, TestStepExecutionContext stepExecutionContext) {
		Document document;
		try {
			document = buildInputSkeleton(stepExecutionContext, testArtefact.getProcedure());
			VariablesManager varMan = ExecutionContext.getCurrentContext().getVariablesManager();
			Map<String, String> inputAttributes = getPayloadAttributes(document);
			for(String key:inputAttributes.keySet()) {
				if(!key.contains(":")) {
					String value = resolveAttribute(testArtefact, varMan, key);
					if(value==null) {
						if(context.getVariablesManager().getVariableAsBoolean("tec.handlers.call.attributes.usedefaults")) {
							value = "";
						} else {
							stepExecutionContext.addMessage(new TechnicalError("Unable to resolve attribute " + key + ". The attribute should either be defined in the map '" + 
									testArtefact.getArguments() + "' or as variable"));							
							return;
						}
					} 
					document.getDocumentElement().setAttribute(key, value);
				}
			}
			
			stepExecutionContext.setInputDocument(document);
		} catch (ParserException e1) {
			stepExecutionContext.addMessage(new TechnicalError("Error parsing input", e1));
		}
	}

	private String resolveAttribute(Call testArtefact, VariablesManager varMan, String key) {
		String value = resolveAttributeFromArgumentsObject(testArtefact, key, varMan);
		if(value==null) {
			try {
				value = varMan.getVariableAsString(key);
			} catch(UndefinedVariableException e) {}
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	private String resolveAttributeFromArgumentsObject(Call testArtefact, String key, VariablesManager varMan) {
		String value = null;
		String mapKey = testArtefact.getArguments();
		if(mapKey!=null) {
			Object args = varMan.getVariable(mapKey);
			if(args!=null) {
				if(args instanceof Map) {
					value = getValueFromMap((Map<String, String>)args,key);
				} else if(args instanceof Collection) {
					for(Object o:(Collection<?>)args) {
						if(o instanceof Map) {
							value = getValueFromMap((Map<String, String>)o,key);
						}
						if(value!=null) {
							break;
						}
					}
				}
			}
		}
		return value;
	}
	
	private String getValueFromMap(Map<String, String> map, String key) {
		String value = null;
		try {
			value = ((Map<String, String>)map).get(key);
		} catch (ValidationException e) {}
		return value;
	}
	
	private File getSchemaFile(String keyword) {
		String schemaLocation = ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsString("tec.schemalocation");
		String schemaBaseDir = Configuration.getInstance().getProperty("tec.schemadir");
		File schemaFile = new File(schemaBaseDir + "/" + schemaLocation + "/" + keyword + ".xsd");
		return schemaFile;
	}
	
	private Document buildInputSkeleton(TestStepExecutionContext stepExecutionContext, String procedure) throws ParserException {
		String dummyContent = SchemaInstanceGeneratorBridge.xsd2inst(new File[] { getSchemaFile(procedure) }, procedure);
		return stepExecutionContext.getParser().parse(dummyContent);
	}
	
	public Map<String, String> getPayloadAttributes(Document document) {
		Map<String, String> result = new HashMap<>();
		if(document!=null && document.getDocumentElement()!=null) {
			NamedNodeMap map = document.getDocumentElement().getAttributes();
			for(int i=0; i<map.getLength(); i++) {
				Node node = map.item(i);
				if(node.getNodeType()==Node.ATTRIBUTE_NODE) {
					result.put(node.getNodeName(), node.getNodeValue());
				}
			}
		}
		return result;
	}
	
	
	@Override
	public TestStepReportNode createReportNode_(ReportNode node, Call testArtefact) {
		return new TestStepReportNode();
	}
	


}
