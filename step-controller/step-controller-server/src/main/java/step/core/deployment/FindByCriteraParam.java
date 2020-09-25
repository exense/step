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
package step.core.deployment;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FindByCriteraParam {
	@JsonCreator
	public FindByCriteraParam(@JsonProperty("criteria") HashMap<String, String> criteria,
			@JsonProperty("start") Date start, @JsonProperty("end") Date end, 
			@JsonProperty("limit") int limit, @JsonProperty("skip") int skip ) throws Exception 
	{
		super();
		this.criteria = criteria;
		this.start = start;
		this.end = end;
		this.limit = limit;
		this.skip = skip;
	}
	public Map<String, String> getCriteria() {
		return criteria;
	}
	public void setCriteria(HashMap<String, String> criteria) {
		this.criteria = criteria;
	}
	public Date getStart() {
		return start;
	}
	public void setStart(Date start) {
		this.start = start;
	}
	public Date getEnd() {
		return end;
	}
	public void setEnd(Date end) {
		this.end = end;
	}
	public int getSkip() {
		return skip;
	}
	public void setSkip(int skip) {
		this.skip = skip;
	}
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	private HashMap<String, String> criteria = new HashMap<>();
	private Date start;
	private Date end;
	private int skip;
	private int limit;
}
