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
package step.common.managedoperations;

import java.util.Date;

public class Operation {

	private String name;
	
	private Date start;
	
	private Object details;
	
	private String reportNodeId;

	private String artefactHash;
	
	private long tid;
	
	public Operation(String name, Date start, Object details, String reportNodeId, String artefactHash, long tid) {
		super();
		this.name = name;
		this.start = start;
		this.details = details;
		this.reportNodeId = reportNodeId;
		this.artefactHash = artefactHash;
		this.tid = tid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Object getDetails() {
		return details;
	}

	public void setDetails(Object details) {
		this.details = details;
	}

	public String getReportNodeId() {
		return reportNodeId;
	}

	public void setReportNodeId(String reportNodeId) {
		this.reportNodeId = reportNodeId;
	}

	public String getArtefactHash() {
		return artefactHash;
	}

	public void setArtefactHash(String artefactHash) {
		this.artefactHash = artefactHash;
	}

	public long getTid() {
		return tid;
	}

	public void setTid(long tid) {
		this.tid = tid;
	}

	@Override
	public String toString() {
		return "Operation{" +
				"name='" + name + '\'' +
				", start=" + start +
				", details=" + details +
				", reportNodeId='" + reportNodeId + '\'' +
				", artefactHash='" + artefactHash + '\'' +
				", tid=" + tid +
				'}';
	}
}
