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

import java.util.Map;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.TokenSession;

public class AbstractScript  {
	
	protected Logger logger = LoggerFactory.getLogger(AbstractScript.class);

	protected OutputMessageBuilder output;
	
	protected JsonObject input;

	// TODO
//	protected Map<String, String> agentProperties;
//	
//	protected Map<String, String> controllerProperties;
	
	protected Map<String, String> properties;
	
	protected TokenSession session;
	
	public TokenSession getSession() {
		return session;
	}

	public void setSession(TokenSession session) {
		this.session = session;
	}

	public JsonObject getInput() {
		return input;
	}

	public void setInput(JsonObject input) {
		this.input = input;
	}

	public OutputMessageBuilder getOutputBuilder() {
		return output;
	}

	public void setOutputBuilder(OutputMessageBuilder outputBuilder) {
		this.output = outputBuilder;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

}
