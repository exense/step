package step.grid.client;

import java.util.Map;

import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;


public class TokenPretender implements Identity {

	final Map<String, String> selectionAttributes;
	
	final Map<String, Interest> interests;
	
	public TokenPretender(Map<String, String> selectionAttributes, Map<String, Interest> interests) {
		super();
		this.selectionAttributes = selectionAttributes;
		this.interests = interests;
	}

	@Override
	public String getID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getAttributes() {
		return selectionAttributes;
	}

	@Override
	public Map<String, Interest> getInterests() {
		return interests;
	}	

	@Override
	public String toString() {
		return "AdapterTokenPretender [attributes="
				+ selectionAttributes + ", selectionCriteria=" + interests + "]";
	}
}
