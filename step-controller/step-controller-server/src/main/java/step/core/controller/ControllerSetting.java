package step.core.controller;

import step.core.accessors.AbstractIdentifiableObject;

public class ControllerSetting extends AbstractIdentifiableObject {

	protected String key;
	
	protected String value;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
