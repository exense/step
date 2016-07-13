package step.grid.agent.tokenpool;

import java.util.HashMap;
import java.util.Map;

public class TokenSession {

	Map<String, Object> attributes = new HashMap<>();

	public Object get(Object arg0) {
		return attributes.get(arg0);
	}

	public Object getOrDefault(Object arg0, Object arg1) {
		return attributes.getOrDefault(arg0, arg1);
	}

	public Object put(String arg0, Object arg1) {
		return attributes.put(arg0, arg1);
	}
}
