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
package step.core.access;

import java.util.HashMap;
import java.util.Map;

public class Preferences {

	Map<String, Object> preferences = new HashMap<>();

	public Preferences() {
		super();
	}

	public Map<String, Object> getPreferences() {
		return preferences;
	}

	public void setPreferences(Map<String, Object> preferences) {
		this.preferences = preferences;
	}

	public String get(String key) {
		return (String) preferences.get(key);
	}

	public String getOrDefault(String key, String defaultValue) {
		return (String) preferences.getOrDefault(key, defaultValue);
	}
	
	public Object getAsBoolean(String key) {
		return (boolean) preferences.get(key);
	}

	public Object getOrDefaultAsBoolean(String key, boolean defaultValue) {
		return (boolean) preferences.getOrDefault(key, defaultValue);
	}

	public Object put(String key, Object value) {
		return preferences.put(key, value);
	}
	
	
}
