package step.plugins.selenium;

import step.core.dynamicbeans.DynamicValue;
import step.plugins.functions.types.ScriptFunction;

public class SeleniumFunction extends ScriptFunction {

	String seleniumVersion = "2.x";
	
	DynamicValue<String> jarFile = new DynamicValue<>();
	
	public String getSeleniumVersion() {
		return seleniumVersion;
	}

	public void setSeleniumVersion(String seleniumVersion) {
		this.seleniumVersion = seleniumVersion;
	}

	public DynamicValue<String> getJarFile() {
		return jarFile;
	}

	public void setJarFile(DynamicValue<String> jarFile) {
		this.jarFile = jarFile;
	}
}
