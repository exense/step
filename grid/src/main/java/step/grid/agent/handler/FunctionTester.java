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
package step.grid.agent.handler;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class FunctionTester {

	public static class Context {
		
		MessageHandler handler;
		
		AgentTokenWrapper token;

		public Context(MessageHandler handler, Map<String, String> properties) {
			super();
			this.handler = handler;
			token = new AgentTokenWrapper();
			if(properties!=null) {
				token.setProperties(properties);
			}
			token.setSession(new TokenSession());
		} 
		
		public OutputMessage run(String function, String argument, Map<String, String> properties) throws Exception {
			return run(function, read(argument), properties);
		}
		
		public OutputMessage run(String function, String argument) {
			return run(function, read(argument), new HashMap<String, String>());
		}

		private JsonObject read(String argument) {
			return Json.createReader(new StringReader(argument)).readObject();
		}
		
		public OutputMessage run(String function, JsonObject argument) throws Exception {
			return run(function, argument, new HashMap<String, String>());
		}
		
		public OutputMessage run(String function, JsonObject argument, Map<String, String> properties) {			
			InputMessage input = new InputMessage();
			input.setFunction(function);
			input.setArgument(argument);			
			input.setProperties(properties);
			input.setCallTimeout(Integer.MAX_VALUE);
			
			try {
				return handler.handle(token, input);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static Context getContext(MessageHandler handler) {
		return new Context(handler, null);
	}
	
	public static Context getContext(MessageHandler handler, Map<String, String> properties) {
		return new Context(handler, properties);
	}
	
}
