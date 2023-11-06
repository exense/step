package step.functions.packages.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import step.functions.Function;

public class FunctionList {

	public String exception;
	public ArrayList<Function> functions;
	public Map<String, Map<String, Object>> automationPackageAttributes;

	public FunctionList() {
		super();
		functions = new ArrayList<Function>();
		automationPackageAttributes = new HashMap<>();
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

	public Map<String, Map<String, Object>> getAutomationPackageAttributes() {
		return automationPackageAttributes;
	}

	public void setAutomationPackageAttributes(Map<String, Map<String, Object>> automationPackageAttributes) {
		this.automationPackageAttributes = automationPackageAttributes;
	}
}
