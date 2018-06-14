package step.plugins.java;

import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;

public class GeneralScriptFunction extends Function {

	DynamicValue<String> scriptFile = new DynamicValue<>("");
	
	DynamicValue<String> scriptLanguage = new DynamicValue<>("");
	
	DynamicValue<String> librariesFile = new DynamicValue<>("");
	
	DynamicValue<String> errorHandlerFile = new DynamicValue<>("");
	
	public DynamicValue<String> getScriptFile() {
		return scriptFile;
	}

	public void setScriptFile(DynamicValue<String> scriptFile) {
		this.scriptFile = scriptFile;
	}

	public DynamicValue<String> getScriptLanguage() {
		return scriptLanguage;
	}

	public void setScriptLanguage(DynamicValue<String> scriptLanguage) {
		this.scriptLanguage = scriptLanguage;
	}

	public DynamicValue<String> getLibrariesFile() {
		return librariesFile;
	}

	public void setLibrariesFile(DynamicValue<String> librariesFile) {
		this.librariesFile = librariesFile;
	}

	public DynamicValue<String> getErrorHandlerFile() {
		return errorHandlerFile;
	}

	public void setErrorHandlerFile(DynamicValue<String> errorHandlerFile) {
		this.errorHandlerFile = errorHandlerFile;
	}
}
