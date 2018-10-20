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

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

public class OutputMessage {
	
	private JsonObject payload;
	
	private AgentError agentError;

	private String error;
	
	private List<Attachment> attachments;
	
	private List<Measure> measures;

	public OutputMessage() {
		super();
	}
	
	public AgentError getAgentError() {
		return agentError;
	}
	
	public void setAgentError(AgentError agentError) {
		this.agentError = agentError;
	}

	public JsonObject getPayload() {
		return payload;
	}

	public void setPayload(JsonObject payload) {
		this.payload = payload;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public boolean addAttachment(Attachment arg0) {
		if(attachments==null) {
			attachments = new ArrayList<>();
		}
		return attachments.add(arg0);
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public List<Measure> getMeasures() {
		return measures;
	}

	public void setMeasures(List<Measure> measures) {
		this.measures = measures;
	}
}
