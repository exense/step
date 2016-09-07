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
