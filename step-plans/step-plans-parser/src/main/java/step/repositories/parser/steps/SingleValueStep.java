package step.repositories.parser.steps;

import step.repositories.parser.AbstractStep;

public class SingleValueStep extends AbstractStep {

	String value;

	public SingleValueStep() {
		super();
	}

	public SingleValueStep(String value) {
		super();
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "Step [name=" + name + ", value=" + value + "]";
	}
	
	
}
