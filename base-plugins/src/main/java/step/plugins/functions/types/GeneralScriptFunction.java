package step.plugins.functions.types;

import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;

public class GeneralScriptFunction extends Function {

	DynamicValue<String> scriptFile = new DynamicValue<>("");
	
	DynamicValue<String> scriptLanguage = new DynamicValue<>("");
	

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
}
