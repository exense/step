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

import javax.json.JsonObject;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;

/**
 * This class encapsulates all the configuration parameters of functions (aka Keywords)
 * which can also be defined on the configuration dialog of Keywords in the UI 
 *
 */
@JsonTypeInfo(use=Id.CLASS,property="type")
public class Function extends AbstractOrganizableObject {
	
	protected DynamicValue<Integer> callTimeout = new DynamicValue<>(180000);
	protected JsonObject schema;
	protected Map<String, String> tokenSelectionCriteria;
	
	public Map<String, String> getTokenSelectionCriteria() {
		return tokenSelectionCriteria;
	}

	/**
	 * Defines additional selection criteria of agent token on which the function should be executed
	 * 
	 * @param tokenSelectionCriteria a map containing the additional selection criteria as key-value pairs
	 */
	public void setTokenSelectionCriteria(Map<String, String> tokenSelectionCriteria) {
		this.tokenSelectionCriteria = tokenSelectionCriteria;
	}

	public static final String NAME = "name";
	
	public DynamicValue<Integer> getCallTimeout() {
		return callTimeout;
	}

	/**
	 * @param callTimeout the call timeout of the function in ms
	 */
	public void setCallTimeout(DynamicValue<Integer> callTimeout) {
		this.callTimeout = callTimeout;
	}
	
	public JsonObject getSchema() {
		return schema;
	}

	/**
	 * Sets the JSON schema to be used to validate the function's input at execution time
	 * 
	 * @param schema the JSON schema of the function. See https://json-schema.org/ for more details concerning JSON schema.
	 */
	public void setSchema(JsonObject schema) {
		this.schema = schema;
	}
	
	public boolean requiresLocalExecution() {
		return false;
	}
}
