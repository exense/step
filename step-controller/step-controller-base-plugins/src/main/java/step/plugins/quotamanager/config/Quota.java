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
package step.plugins.quotamanager.config;


public class Quota {

	private String id;
	
	private String description;
	
	private String quotaKeyFunction;
	
	private int permits;
	
	private Long acquireTimeoutMs;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getQuotaKeyFunction() {
		return quotaKeyFunction;
	}

	public void setQuotaKeyFunction(String quotaKeyFunction) {
		this.quotaKeyFunction = quotaKeyFunction;
	}

	public int getPermits() {
		return permits;
	}

	public void setPermits(int permits) {
		this.permits = permits;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getAcquireTimeoutMs() {
		return acquireTimeoutMs;
	}

	public void setAcquireTimeoutMs(long acquireTimeoutMs) {
		this.acquireTimeoutMs = acquireTimeoutMs;
	}

	@Override
	public String toString() {
		return "Quota [id=" + id + ", description=" + description
				+ ", quotaKeyFunction=" + quotaKeyFunction + ", permits="
				+ permits + ", acquireTimeoutMs=" + acquireTimeoutMs + "]";
	}
}
