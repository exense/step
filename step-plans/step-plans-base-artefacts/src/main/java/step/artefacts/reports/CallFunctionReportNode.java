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
package step.artefacts.reports;

import java.util.List;
import java.util.Map;

import jakarta.json.JsonObject;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNode;
import step.core.reports.Measure;

public class CallFunctionReportNode extends ReportNode {

	protected String functionId;
	
	protected Map<String, String> functionAttributes;

	protected String agentUrl;
	
	protected String tokenId;
	
	protected String input;
	
	protected String output;
	
	@JsonIgnore
	protected JsonObject outputObject;
	
	private List<Measure> measures;

	public CallFunctionReportNode() {
		super();
	}

	public String getAgentUrl() {
		return agentUrl;
	}

	public void setAgentUrl(String agentUrl) {
		this.agentUrl = agentUrl;
	}

	public String getTokenId() {
		return tokenId;
	}

	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public JsonObject getOutputObject() {
		return outputObject;
	}

	public void setOutputObject(JsonObject outputObject) {
		this.outputObject = outputObject;
	}

	public String getFunctionId() {
		return functionId;
	}

	public void setFunctionId(String functionId) {
		this.functionId = functionId;
	}

	public Map<String, String> getFunctionAttributes() {
		return functionAttributes;
	}

	public void setFunctionAttributes(Map<String, String> functionAttributes) {
		this.functionAttributes = functionAttributes;
	}

	public List<Measure> getMeasures() {
		return measures;
	}

	public void setMeasures(List<Measure> measures) {
		this.measures = measures;
	}

	@Override
	public String getReportAsString() {
		String report = "Input=" + input + ", Output=" + output;
		String kwName = functionAttributes.get(AbstractOrganizableObject.NAME);
		return (this.getName().equals(kwName)) ?
				report :
				"Keyword name=" + kwName + ", " + report;
	}
}
