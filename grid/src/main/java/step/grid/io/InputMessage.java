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
package step.grid.io;

import java.util.Map;

import javax.json.JsonObject;

public class InputMessage {

	private String function;
	
	private String handler;

	private JsonObject argument;
	
	private Map<String, String> properties;
	
	private int callTimeout;

	public InputMessage() {
		super();
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getHandler() {
		return handler;
	}

	public void setHandler(String handler) {
		this.handler = handler;
	}

	public JsonObject getArgument() {
		return argument;
	}

	public void setArgument(JsonObject argument) {
		this.argument = argument;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public int getCallTimeout() {
		return callTimeout;
	}

	public void setCallTimeout(int callTimeout) {
		this.callTimeout = callTimeout;
	}
	
	
}
