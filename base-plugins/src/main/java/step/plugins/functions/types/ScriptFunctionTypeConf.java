package step.plugins.functions.types;

import step.functions.type.FunctionTypeConf;

public class ScriptFunctionTypeConf extends FunctionTypeConf {
	
	String scriptDir;

	public String getScriptDir() {
		return scriptDir;
	}

	public void setScriptDir(String scriptDir) {
		this.scriptDir = scriptDir;
	}
}
