package step.plugins.selenium;

import java.io.File;
import java.util.Map;

import step.commons.conf.Configuration;
import step.core.dynamicbeans.DynamicValue;
import step.functions.type.SetupFunctionException;
import step.plugins.java.AbstractScriptFunctionType;

public class SeleniumFunctionType extends AbstractScriptFunctionType<SeleniumFunction> {

	protected final Configuration configuration;
	
	public SeleniumFunctionType(Configuration configuration) {
		super();
		this.configuration = configuration;
	}

	@Override
	public Map<String, String> getHandlerProperties(SeleniumFunction function) {
		String seleniumVersion = function.getSeleniumVersion();
		
		String propertyName = "plugins.selenium.libs."+seleniumVersion;
		String seleniumLibPath = configuration.getProperty(propertyName);
		if(seleniumLibPath==null) {
			throw new RuntimeException("Property '"+propertyName+"' in step.properties isn't set. Please set it to path of the installation folder of selenium");
		}
		
		File seleniumLibFile = new File(seleniumLibPath);
		if(!seleniumLibFile.exists()) {
			throw new RuntimeException("The path to the selenium installation doesn't exist: "+seleniumLibFile.getAbsolutePath());
		}
		
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
