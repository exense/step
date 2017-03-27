package step.plugins.selenium;

import java.io.File;
import java.util.Map;

import step.functions.type.AbstractFunctionType;
import step.functions.type.SetupFunctionException;
import step.plugins.functions.types.ScriptFunctionTypeHelper;

public class SeleniumFunctionType extends AbstractFunctionType<SeleniumFunction> {

	ScriptFunctionTypeHelper helper;
	
	@Override
	public void init() {
		super.init();
		helper = new ScriptFunctionTypeHelper(getContext());
	}
	
	@Override
	public String getHandlerChain(SeleniumFunction function) {
		return "class:"+SeleniumHandler.class.getName();
	}

	@Override
	public Map<String, String> getHandlerProperties(SeleniumFunction function) {		
		Map<String,String> handlerProperties = helper.getHandlerProperties(function);
		
		handlerProperties.put(SeleniumHandler.SELENIUM_VERSION, function.getSeleniumVersion());
		
		return handlerProperties;
	}

	@Override
	public void setupFunction(SeleniumFunction function) throws SetupFunctionException {
		if(function.getScriptLanguage().get().equals("javascript")) {
			File scriptFile = helper.setupScriptFile(function);
			helper.createScriptFromTemplate(scriptFile, "kw_selenium.js");			
		}
	}
	
	@Override
	public SeleniumFunction newFunction() {
		SeleniumFunction function = new SeleniumFunction();
		function.getScriptLanguage().setValue("javascript");
		return function;
	}
}
