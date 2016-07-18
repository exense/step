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
