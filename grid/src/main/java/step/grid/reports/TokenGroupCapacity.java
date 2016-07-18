package step.grid.reports;

import java.util.Map;

public class TokenGroupCapacity {

	Map<String, String> key;
	
	int usage = 0;
	
	int capacity = 0;
	
	public TokenGroupCapacity(Map<String, String> key) {
		super();
		this.key = key;
	}

	public void incrementUsage() {
		usage++;
	}
	
	public void incrementCapacity() {
		capacity++;
	}

	public Map<String, String> getKey() {
		return key;
	}

	public int getUsage() {
		return usage;
	}

	public int getCapacity() {
		return capacity;
	}
	
}
