package step.plugins.selenium;

import step.plugins.java.GeneralScriptFunction;

public class SeleniumFunction extends GeneralScriptFunction {

	String seleniumVersion = "2.x";
	
	public String getSeleniumVersion() {
		return seleniumVersion;
	}

	public void setSeleniumVersion(String seleniumVersion) {
		this.seleniumVersion = seleniumVersion;
	}
}
