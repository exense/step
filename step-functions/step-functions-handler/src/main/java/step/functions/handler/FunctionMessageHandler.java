package step.functions.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.contextbuilder.RemoteApplicationContextFactory;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class FunctionMessageHandler extends AbstractMessageHandler {

	public static final String FUNCTION_HANDLER_PACKAGE_KEY = "$functionhandlerjar";
	
	public static final String FUNCTION_HANDLER_KEY = "$functionhandler";
	
	// Cached object mapper for message payload serialization
	private ObjectMapper mapper;
	
	public FunctionHandlerFactory functionHandlerFactory = new FunctionHandlerFactory();
	
	public FunctionMessageHandler() {
		super();
		
		mapper = FunctionInputOutputObjectMapperFactory.createObjectMapper();
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage inputMessage) throws Exception {
		FileVersionId functionPackage = getFileVersionId(FUNCTION_HANDLER_PACKAGE_KEY, inputMessage.getProperties());
		if(functionPackage != null) {
			RemoteApplicationContextFactory functionHandlerContext = new RemoteApplicationContextFactory(token.getServices().getFileManagerClient(), getFileVersionId(FUNCTION_HANDLER_PACKAGE_KEY, inputMessage.getProperties()));
			token.getServices().getApplicationContextBuilder().pushContext(functionHandlerContext);
		}
		
		return token.getServices().getApplicationContextBuilder().runInContext(()->{
			// Instantiate the function handler 
			String handlerClass = inputMessage.getProperties().get(FUNCTION_HANDLER_KEY);
			AbstractFunctionHandler functionHandler = functionHandlerFactory.create(token, handlerClass);
			
			// Deserialize the Input from the message payload
			Input<?> input = mapper.treeToValue(inputMessage.getPayload(), Input.class);
			
			// Handle the input
			Output<?> output = functionHandler.handle(input);
			
			// Serialize the output
			ObjectNode outputPayload = (ObjectNode) mapper.valueToTree(output);

			// Create and return the output message 
			OutputMessageBuilder outputMessageBuilder = new OutputMessageBuilder();
			outputMessageBuilder.setPayload(outputPayload);
			return outputMessageBuilder.build();
			
		});
	}
}
