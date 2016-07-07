package step.grid.agent.conf;

import java.util.HashMap;
import java.util.List;


public class Adapter {
	
	public static final String PASSIVE_ADAPTER_KEY = "PASSIVE";
	
	List<AdapterTokenGroup> groups;
		
	HashMap<String, String> properties;

	public HashMap<String, String> getProperties() {
		return properties;
	}

	public List<AdapterTokenGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<AdapterTokenGroup> groups) {
		this.groups = groups;
	}

	public void setProperties(HashMap<String, String> properties) {
		this.properties = properties;
	}

}
