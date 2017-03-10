package step.plugins.functions.types;

import java.io.File;
import java.util.Map;

import step.functions.type.AbstractFunctionType;
import step.functions.type.SetupFunctionException;
import step.handlers.GeneralScriptHandler;

public class GeneralScriptFunctionType extends AbstractFunctionType<GeneralScriptFunction> {
	
	ScriptFunctionTypeHelper helper;
	
	@Override
	public void init() {
		super.init();
		helper = new ScriptFunctionTypeHelper(getContext());
	}

	@Override
	public String getHandlerChain(GeneralScriptFunction function) {
		return "class:"+GeneralScriptHandler.class.getName();
	}

	@Override
	public Map<String, String> getHandlerProperties(GeneralScriptFunction function) {
		return helper.getHandlerProperties(function);
	}

	@Override
	public void setupFunction(GeneralScriptFunction function) throws SetupFunctionException {
		String language = helper.getScriptLanguage(function);
		if(language.equals("java")) {
			// No specific setup for java at the moment
		} else {
			File scriptFile = helper.setupScriptFile(function);
			if(language.equals("javascript")) {
				helper.createScriptFromTemplate(scriptFile, "custom_script.js");
			} else if(language.equals("groovy")) {
				helper.createScriptFromTemplate(scriptFile, "custom_script.groovy");
			}			
		}
	}

	@Override
	public GeneralScriptFunction newFunction() {
		GeneralScriptFunction function = new GeneralScriptFunction();
		function.getScriptLanguage().setValue("javascript");
		return function;
	}
}
