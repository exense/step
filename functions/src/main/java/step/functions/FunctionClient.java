package step.functions;

import java.util.Map;

import step.grid.client.GridClient;
import step.grid.client.GridClient.TokenFacade;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionClient {

	private GridClient gridClient;
	
	private FunctionRepository functionRepository;
	
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
		
		public void release() {
			releaseFunctionToken(this);
		}
	}
	
	private Output callFunction(FunctionToken functionToken, String functionId, Input input) {
		
		Function function = functionRepository.getFunctionById(functionId);
		
		FunctionConfiguration functionConf = functionRepository.getFunctionConfigurationById(functionId);
		
		String handlerChain = functionConf.getHandlerChain();
		
		try {
			OutputMessage outputMessage = functionToken.getToken().process(function.getName(), input.getArgument(), handlerChain);
			Output output = new Output();
			output.setResult(outputMessage.getPayload());
			return output;
		} catch (Exception e) {
			// TODO typed error
			throw new RuntimeException("Unmapped error");
		}
		
	}
	
	private void releaseFunctionToken(FunctionToken functionToken) {
		functionToken.getToken().release();
	}
	
}
