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
package step.handlers.javahandler;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.agent.AgentTokenServices;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class KeywordRunner {
	
	private static final Logger logger = LoggerFactory.getLogger(KeywordRunner.class);

	public static class ExecutionContext {
		
		AgentTokenWrapper token;
		
		KeywordHandler handler;
		
		List<Class<?>> functionClasses;
		
		public ExecutionContext(List<Class<?>> functionClasses, Map<String, String> properties) {
			super();
			this.functionClasses = functionClasses;
			token = new AgentTokenWrapper();
			if(properties!=null) {
				token.setProperties(properties);
			}
			token.setTokenReservationSession(new TokenReservationSession());
			
			AgentTokenServices tokenServices = new AgentTokenServices(null);
			tokenServices.setApplicationContextBuilder(new ApplicationContextBuilder());
			token.setServices(tokenServices);
			handler = new KeywordHandler();
			handler.init(tokenServices);
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
		
		public OutputMessage run(String function) throws Exception {
			return run(function, Json.createObjectBuilder().build(), new HashMap<String, String>());
		}
		
		public OutputMessage run(String function, JsonObject argument) throws Exception {
			return run(function, argument, new HashMap<String, String>());
		}
		
		public OutputMessage run(String function, JsonObject argument, Map<String, String> properties) {
			return execute(function, argument, properties);
		}

		private OutputMessage execute(String function, JsonObject argument, Map<String, String> properties) {
			InputMessage input = new InputMessage();
			input.setFunction(function);
			input.setArgument(argument);			
			StringBuilder classes = new StringBuilder();
			functionClasses.forEach(cl->{classes.append(cl.getName()+";");});
			properties.put(KeywordHandler.KEYWORD_CLASSES, classes.toString());
			input.setProperties(properties);
			
			OutputMessage output;
			try {
				output = handler.handle(token, input);
				if(output.getError()!=null) {
					logger.error("Keyword error occurred:"+output.getError());
				}
				return output;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		public void close() {
			token.getSession().close();
			token.getTokenReservationSession().close();
		}
	}
	
	public static ExecutionContext getExecutionContext(Class<?>... functionClass) {
		return getExecutionContext(new HashMap<>(), functionClass);
	}
	
	public static ExecutionContext getExecutionContext(Map<String, String> properties, Class<?>... keywordClass) {
		if(keywordClass.length==0) {
			throw new RuntimeException("Please specify at leat one class containing the keyword definitions");
		}
		return new ExecutionContext(Arrays.asList(keywordClass), properties);
	}
	
}
