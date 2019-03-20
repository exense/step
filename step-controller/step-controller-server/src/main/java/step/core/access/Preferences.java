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
