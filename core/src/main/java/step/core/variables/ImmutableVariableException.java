package step.core.variables;

public class ImmutableVariableException extends RuntimeException {

	private static final long serialVersionUID = -7770455970260059111L;

	public ImmutableVariableException() {
		super();
	}

	public ImmutableVariableException(String variableName) {
		super("The variable " + variableName + " is immutable.");
	}

}
