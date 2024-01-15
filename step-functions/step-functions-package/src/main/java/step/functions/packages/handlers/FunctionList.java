package step.functions.packages.handlers;

import java.util.ArrayList;

import step.functions.Function;

public class FunctionList {

	public String exception;
	public ArrayList<Function> functions;

	public FunctionList() {
		super();
		functions = new ArrayList<Function>();
	}

	public ArrayList<Function> getFunctions() {
		return functions;
	}

	public void setFunctions(ArrayList<Function> functions) {
		this.functions = functions;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}
}
