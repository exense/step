package step.plugins.screentemplating;

import step.core.accessors.AbstractOrganizableObject;

public class ScreenInput extends AbstractOrganizableObject {

	protected String screenId;
	
	protected Input input;

	public ScreenInput() {
		super();
	}

	public ScreenInput(String screenId, Input input) {
		super();
		this.screenId = screenId;
		this.input = input;
	}

	public String getScreenId() {
		return screenId;
	}

	public void setScreenId(String screenId) {
		this.screenId = screenId;
	}

	public Input getInput() {
		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}
	
}
