package step.plugins.functions.types;

import step.functions.type.FunctionTypeConf;

public class ScriptFunctionTypeConf extends FunctionTypeConf {

	private String scriptLanguage;

	public String getScriptLanguage() {
		return scriptLanguage;
	}

	public void setScriptLanguage(String scriptLanguage) {
		this.scriptLanguage = scriptLanguage;
	}
}
