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

import step.grid.client.GridClient;
import step.grid.client.GridClient.TokenHandle;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionClient {

	private final GridClient gridClient;
	
	private final FunctionRepository functionRepository;
	
	public FunctionClient(GridClient gridClient, FunctionRepository functionRepository) {
		super();
		this.gridClient = gridClient;
		this.functionRepository = functionRepository;
	}
	
	public FunctionTokenHandle getLocalFunctionToken() {
		return new FunctionTokenHandle(gridClient.getLocalToken());
	}

	public FunctionTokenHandle getFunctionToken(Map<String, String> attributes, Map<String, Interest> interest) {
		return new FunctionTokenHandle(gridClient.getToken(attributes, interest));
	}
	
	public FunctionTokenHandle getFunctionToken() {
		return new FunctionTokenHandle(gridClient.getToken());
	}
	
	public class FunctionTokenHandle {
		
		TokenHandle tokenHandle;

		private FunctionTokenHandle(TokenHandle tokenHandle) {
			super();
			this.tokenHandle = tokenHandle;
		}
		
		public Output call(Map<String, String> attributes, Input input) {
			Function function = functionRepository.getFunctionByAttributes(attributes);
			return callFunction(tokenHandle, function, input);
		}
		
		public Output call(String functionId, Input input) {
			Function function = functionRepository.getFunctionById(functionId);
			return callFunction(tokenHandle, function, input);
		}
		
		public void release() {
			tokenHandle.release();		
			tokenHandle.setCurrentOwner(null);
		}
		
		public void setCurrentOwner(Object owner) {
			tokenHandle.setCurrentOwner(owner);
		}

		@Override
		public String toString() {
			return tokenHandle.getToken().getID();
		}
		
		
	}

	private Output callFunction(TokenHandle tokenHandle, Function function, Input input) {
		String handlerChain = function.getHandlerChain();

		Output output = new Output();
		output.setFunction(function);
		try {
			TokenHandle facade = tokenHandle.setHandler(handlerChain).addProperties(input.getProperties())
					.addProperties(function.getHandlerProperties());
			if(function.getCallTimeout()!=null) {
				facade.setCallTimeout(function.getCallTimeout());
			}
			OutputMessage outputMessage = facade.process(function.getAttributes().get("name"), input.getArgument());		
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

	public FunctionRepository getFunctionRepository() {
		return functionRepository;
	}
	
}
