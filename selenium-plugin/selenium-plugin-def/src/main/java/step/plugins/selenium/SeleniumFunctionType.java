package step.plugins.selenium;

import java.util.Map;

import step.core.dynamicbeans.DynamicValue;
import step.functions.type.SetupFunctionException;
import step.plugins.java.AbstractScriptFunctionType;

public class SeleniumFunctionType extends AbstractScriptFunctionType<SeleniumFunction> {

	@Override
	public Map<String, String> getHandlerProperties(SeleniumFunction function) {
		String seleniumVersion = function.getSeleniumVersion();
		String seleniumLibPath = getContext().getConfiguration().getProperty("plugins.selenium.libs."+seleniumVersion);
		function.setLibrariesFile(new DynamicValue<String>(seleniumLibPath));
		return super.getHandlerProperties(function);
	}

	@Override
	public void setupFunction(SeleniumFunction function) throws SetupFunctionException {
		if(function.getScriptLanguage().get().equals("javascript")) {
			setupScriptFile(function,"kw_selenium.js");
		}
	}

	@Override
	public SeleniumFunction newFunction() {
		SeleniumFunction function = new SeleniumFunction();
		function.getScriptLanguage().setValue("javascript");
		return function;
	}
}
