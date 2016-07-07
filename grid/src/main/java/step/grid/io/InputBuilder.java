package step.grid.io;

import java.util.HashMap;

public class InputBuilder extends AdapterMessageBuilder<Input> {
	
	private HashMap<String, String> parameters = new HashMap<>();
	
	public InputBuilder() {
		super(new Input());		
	}
	
	public InputBuilder(String tagName) {
		this();
		createDocument(tagName);
	}
	
	public void setUserID(String userID) {
		message.setUserID(userID);
	}
	
	public void addParameter(String key, String value) {
		parameters.put(key, value);
	}

	@Override
	public Input build() {
		message.setParameters(parameters);
		return super.build();
	}
}
