/*******************************************************************************
 * (C) Copyright 2016 Dorian Cransac and Jerome Comte
 *  
 * This file is part of rtm
 *  
 * rtm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * rtm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with rtm.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.plugins.events;

import java.util.Map;

public class Event {
	
	private String id;
	private String name;
	private String group;
	private Map<String, Object> payload;
	
	private final long creationTimestamp;
	
	//TODO turn these timestamps into generic map of <String,Long> ?
	
	// For client side
	private long submitionTimestamp;
	private long receptionTimestamp;
	
	// Server side
	private long insertionTimestamp;
	private long deletionTimestamp;
	private long lastReadTimestamp;
	
	public Event(){
		creationTimestamp = System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}

	public Event setId(String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public Event setName(String name) {
		this.name = name;
		return this;
	}

	public String getGroup() {
		return group;
	}
	
	public Event setGroup(String group) {
		this.group = group;
		return this;
	}
	public Map<String, Object> getPayload() {
		return payload;
	}
	
	public Event setPayload(Map<String, Object> payload) {
		this.payload = payload;
		return this;
	}

	public String toString(){
		return new StringBuilder()
				.append("{")
				.append("id=").append(id).append(",")
				.append("group=").append(group).append(",")
				.append("name=").append(name).append(",")
				.append("payload=").append(payload)
				.append("}")
				.toString();
	}

	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	public long getSubmitionTimestamp() {
		return submitionTimestamp;
	}

	public Event setSubmitionTimestamp(long submitionTimestamp) {
		this.submitionTimestamp = submitionTimestamp;
		return this;
	}

	public long getReceptionTimestamp() {
		return receptionTimestamp;
	}

	public Event setReceptionTimestamp(long receptionTimestamp) {
		this.receptionTimestamp = receptionTimestamp;
		return this;
	}

	public long getInsertionTimestamp() {
		return insertionTimestamp;
	}

	public Event setInsertionTimestamp(long insertionTimestamp) {
		this.insertionTimestamp = insertionTimestamp;
		return this;
	}

	public long getDeletionTimestamp() {
		return deletionTimestamp;
	}

	public Event setDeletionTimestamp(long deletionTimestamp) {
		this.deletionTimestamp = deletionTimestamp;
		return this;
	}

	public long getLastReadTimestamp() {
		return lastReadTimestamp;
	}

	public Event setLastReadTimestamp(long lastReadTimestamp) {
		this.lastReadTimestamp = lastReadTimestamp;
		return this;
	}

}
