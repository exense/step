package step.core.variables;

public class UndefinedVariableException extends RuntimeException {

	private static final long serialVersionUID = -7770455970260059111L;

	public UndefinedVariableException() {
		super();
	}

	public UndefinedVariableException(String variableName) {
		super("The variable " + variableName + " is undefined.");
	}

}
