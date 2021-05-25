/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.functions;

import java.util.Map;

import javax.json.JsonObject;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import ch.exense.commons.core.model.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.json.JsonProviderCache;

/**
 * This class encapsulates all the configuration parameters of functions (aka Keywords)
 * which can also be defined on the configuration dialog of Keywords in the UI 
 *
 */
@JsonTypeInfo(use=Id.CLASS,property="type")
public class Function extends AbstractOrganizableObject {
	
	protected DynamicValue<Integer> callTimeout = new DynamicValue<>(180000);
	protected JsonObject schema = JsonProviderCache.createObjectBuilder().build();
	
	protected boolean executeLocally;
	protected Map<String, String> tokenSelectionCriteria;
	
	protected boolean managed;
	
	protected boolean useCustomTemplate=false;
	protected String htmlTemplate="";
	
	protected String description;
	
	public static final String APPLICATION = "application";
	
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

	/**
	 * @return if the function has to be executed on a local token
	 */
	public boolean isExecuteLocally() {
		return executeLocally;
	}

	/**
	 * Defines if the function has to be executed on a local token
	 * 
	 * @param executeLocally true if the function has to be executed on a local token
	 */
	public void setExecuteLocally(boolean executeLocally) {
		this.executeLocally = executeLocally;
	}
	
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
		return executeLocally;
	}

	public boolean isManaged() {
		return managed;
	}

	public void setManaged(boolean managed) {
		this.managed = managed;
	}

	public boolean isUseCustomTemplate() {
		return useCustomTemplate;
	}

	public void setUseCustomTemplate(boolean customTemplate) {
		this.useCustomTemplate = customTemplate;
	}

	public String getHtmlTemplate() {
		return htmlTemplate;
	}

	/**
	 * Sets the HTML code to be used as template when editing the function in the plan editor
	 * 
	 * @param customTemplateContent the HTML template
	 */
	public void setHtmlTemplate(String customTemplateContent) {
		this.htmlTemplate = customTemplateContent;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
