package step.grid.agent.conf;

import java.util.Map;

public class AdapterTokenGroup {
	
	Map<String, String> attributes;
	
	Map<String, String> selectionPatterns;
	
	Map<String, String> properties;
			
	int capacity;

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public Map<String, String> getSelectionPatterns() {
		return selectionPatterns;
	}

	public void setSelectionPatterns(Map<String, String> selectionPatterns) {
		this.selectionPatterns = selectionPatterns;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}	
}
