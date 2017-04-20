package step.plugins.functions.types;

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
			String template = null;
			if(language.equals("javascript")) {
				template = "custom_script.js";
			} else if(language.equals("groovy")) {
				template = "custom_script.groovy";
			}			
			helper.setupScriptFile(function, template);
		}
	}

	@Override
	public GeneralScriptFunction newFunction() {
		GeneralScriptFunction function = new GeneralScriptFunction();
		function.getScriptLanguage().setValue("javascript");
		return function;
	}
}
