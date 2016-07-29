package step.functions;

import java.util.Map;

import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.PropertyAwareMessageHandler;
import step.grid.agent.handler.TokenHandlerPool;
import step.grid.client.GridClient;
import step.grid.client.GridClient.TokenFacade;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionClient {

	private GridClient gridClient;
	
	private FunctionRepository functionRepository;
	
	public FunctionClient(GridClient gridClient, FunctionRepository functionRepository) {
		super();
		this.gridClient = gridClient;
		this.functionRepository = functionRepository;
	}
	
	public FunctionToken getLocalFunctionToken() {
		return new FunctionToken(null);
	}

	public FunctionToken getFunctionToken(Map<String, String> attributes, Map<String, Interest> interest) {
		return new FunctionToken(gridClient.getToken(attributes, interest));
	}
	
	public class FunctionToken {

		private final TokenFacade token;

		public FunctionToken(TokenFacade token) {
			super();
			this.token = token;
		}

		protected TokenFacade getToken() {
			return token;
		}
		
		public Output call(String functionId, Input input) {
			return callFunction(this, functionId, input);
		}
		
		public Output call(Map<String, String> attributes, Input input) {
			return callFunction(this, attributes, input);
		}
		
		
		public void release() {
			releaseFunctionToken(this);
		}
	}

	public Output callFunction(FunctionToken functionToken, Map<String, String> attributes, Input input) {
		Function function = functionRepository.getFunctionByAttributes(attributes);
		return callFunction(functionToken, function, input);
	}
	
	private Output callFunction(FunctionToken functionToken, String functionId, Input input) {
		Function function = functionRepository.getFunctionById(functionId);
		return callFunction(functionToken, function, input);
	}

	private Output callFunction(FunctionToken functionToken, Function function, Input input) {
		String handlerChain = function.getHandlerChain();

		try {
			OutputMessage outputMessage;
			if(functionToken.getToken()!=null) {
				outputMessage = functionToken.getToken().process(function.getAttributes().get("name"), input.getArgument(), handlerChain);		
			} else {
				TokenHandlerPool p = new TokenHandlerPool();
				MessageHandler h = p.get(handlerChain);
				InputMessage inputMessage = new InputMessage();
				inputMessage.setArgument(input.getArgument());
				if(h instanceof PropertyAwareMessageHandler) {
					outputMessage = ((PropertyAwareMessageHandler)h).handle(null, function.getHandlerProperties(), inputMessage);
				} else {
					outputMessage = h.handle(null, inputMessage);
				}
			}
			Output output = new Output();
			output.setResult(outputMessage.getPayload());
			output.setError(outputMessage.getError());
			output.setAttachments(outputMessage.getAttachments());
			return output;
		} catch (Exception e) {
			// TODO typed error
			throw new RuntimeException("Unmapped error");
		}
	}
	
	private void releaseFunctionToken(FunctionToken functionToken) {
		if(functionToken.getToken()!=null) {
			functionToken.getToken().release();			
		}
	}

	public FunctionRepository getFunctionRepository() {
		return functionRepository;
	}
	
}
