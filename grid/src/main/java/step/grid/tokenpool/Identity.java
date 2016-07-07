package step.grid.tokenpool;

import java.util.Map;

public interface Identity {

	public String getID();
	
	public Map<String, String> getAttributes();
	
	public Map<String, Interest> getInterests();
}
