/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
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

		public TokenFacade getToken() {
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

		Output output = new Output();
		output.setFunction(function);
		try {
			OutputMessage outputMessage;
			if(functionToken.getToken()!=null) {
				TokenFacade facade = functionToken.getToken().setHandler(handlerChain).setProperties(input.getProperties());
				if(function.getCallTimeout()!=null) {
					facade.setCallTimeout(function.getCallTimeout());
				}
				outputMessage = facade.process(function.getAttributes().get("name"), input.getArgument());		
			} else {
				TokenHandlerPool p = new TokenHandlerPool();
				MessageHandler h = p.get(handlerChain);
				InputMessage inputMessage = new InputMessage();
				inputMessage.setFunction(function.getAttributes().get("name"));
				inputMessage.setArgument(input.getArgument());
				if(h instanceof PropertyAwareMessageHandler) {
					outputMessage = ((PropertyAwareMessageHandler)h).handle(null, function.getHandlerProperties(), inputMessage);
				} else {
					outputMessage = h.handle(null, inputMessage);
				}
			}
			output.setResult(outputMessage.getPayload());
			output.setError(outputMessage.getError());
			output.setAttachments(outputMessage.getAttachments());
			output.setMeasures(outputMessage.getMeasures());
			return output;
		} catch (Exception e) {
			output.setError(e.getClass().getName() + " " + e.getMessage());
			// TODO 
		}
		return output;
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
