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
package step.functions.services;

import java.util.Map;

import step.grid.tokenpool.Interest;

public class GetTokenHandleParameter {
	
	Map<String, String> attributes;
	Map<String, Interest> interests;
	boolean createSession;
	boolean local;
	String reservationDescription;

	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
	
	public Map<String, Interest> getInterests() {
		return interests;
	}
	
	public void setInterests(Map<String, Interest> interests) {
		this.interests = interests;
	}
	
	public boolean isCreateSession() {
		return createSession;
	}
	
	public void setCreateSession(boolean createSession) {
		this.createSession = createSession;
	}

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	public String getReservationDescription() {
		return reservationDescription;
	}

	public void setReservationDescription(String reservationDescription) {
		this.reservationDescription = reservationDescription;
	}
}
