package step.core.variables;

public class Variable {

	private VariableType type;
	
	private Object value;
	
	public Variable(Object value, VariableType type) {
		super();
		this.type = type;
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public VariableType getType() {
		return type;
	}

	public void setType(VariableType type) {
		this.type = type;
	}
}
