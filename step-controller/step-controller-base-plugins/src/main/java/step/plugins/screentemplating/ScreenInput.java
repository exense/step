package step.plugins.screentemplating;

import step.core.accessors.AbstractOrganizableObject;

public class ScreenInput extends AbstractOrganizableObject {

	protected String screenId;
	
	protected int position;
	
	protected Input input;

	public ScreenInput() {
		super();
	}

	public ScreenInput(int position, String screenId, Input input) {
		super();
		this.position = position;
		this.screenId = screenId;
		this.input = input;
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

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public Input getInput() {
		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}
	
}
