package step.script;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ScriptRunner {

	public static class ScriptContext {
		
		AgentTokenWrapper token;

		public ScriptContext() {
			super();
			token = new AgentTokenWrapper();
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
			AnnotatedMethodHandler handler = new AnnotatedMethodHandler(true);
			
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
	
	public static ScriptContext getExecutionContext() {
		return new ScriptContext();
	}
	
}
