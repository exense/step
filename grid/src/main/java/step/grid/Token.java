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
package step.grid;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.grid.tokenpool.Interest;



public class Token {
	
	String id;
	
	String agentid;
	
	Map<String, String> attributes;
	
	Map<String, Interest> selectionPatterns;
	
	Map<String, Object> contextObjects;

	public Token() {
		super();
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getAgentid() {
		return agentid;
	}

	public void setAgentid(String agentid) {
		this.agentid = agentid;
	}
	
	@JsonIgnore
	public boolean isLocal() {
		return agentid.equals("local");
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public Map<String, Interest> getSelectionPatterns() {
		return selectionPatterns;
	}

	public void setSelectionPatterns(Map<String, Interest> selectionPatterns) {
		this.selectionPatterns = selectionPatterns;
	}
	
	
	public Object getAttachedObject(Object key) {
		return contextObjects!=null?contextObjects.get(key):null;
	}

	public synchronized Object attachObject(String key, Object value) {
		if(contextObjects==null) {
			contextObjects = new HashMap<>();
		}
		return contextObjects.put(key, value);
	}
}
