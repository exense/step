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
package step.grid.tokenpool;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class IdentityImpl implements Identity {
	
	Map<String, String> attributes = new HashMap<String, String>();
	
	Map<String, Interest> interests = new HashMap<>();
	
	String id = UUID.randomUUID().toString();
	
	volatile boolean used = false;

	public void addAttribute(String key, String value) {
		attributes.put(key, value);
	}

	public void addInterest(String key, Interest e) {
		interests.put(key, e);
	}

	@Override
	public Map<String, String> getAttributes() {
		return attributes;
	}

	@Override
	public Map<String, Interest> getInterests() {
		return interests;
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public String toString() {
		return "IdentityImpl [attributes=" + attributes + ", interests="
				+ interests + ", id=" + id + ", used=" + used + "]";
	}

}
