package step.plugins.functions.types;

import step.functions.type.FunctionTypeConf;

public class GrinderFunctionTypeConf extends FunctionTypeConf {

	String jythonLibPath;
	
	String grinderLibPath;
	
	String scriptDir;

	public String getJythonLibPath() {
		return jythonLibPath;
	}

	public void setJythonLibPath(String jythonLibPath) {
		this.jythonLibPath = jythonLibPath;
	}

	public String getGrinderLibPath() {
		return grinderLibPath;
	}

	public void setGrinderLibPath(String grinderLibPath) {
		this.grinderLibPath = grinderLibPath;
	}

	public String getScriptDir() {
		return scriptDir;
	}

	public void setScriptDir(String scriptDir) {
		this.scriptDir = scriptDir;
	}
}
