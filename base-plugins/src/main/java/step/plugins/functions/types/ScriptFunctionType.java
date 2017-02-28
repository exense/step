package step.plugins.functions.types;

import java.io.File;
import java.util.Map;

import step.functions.type.AbstractFunctionType;
import step.functions.type.SetupFunctionException;

public class ScriptFunctionType extends AbstractFunctionType<ScriptFunction> {
	
	ScriptFunctionTypeHelper helper;
	
	@Override
	public void init() {
		super.init();
		helper = new ScriptFunctionTypeHelper(getContext());
	}

	@Override
	public String getHandlerChain(ScriptFunction function) {
		return "class:step.handlers.scripthandler.ScriptHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(ScriptFunction function) {
		return helper.getHandlerProperties(function);
	}

	@Override
	public void setupFunction(ScriptFunction function) throws SetupFunctionException {
		File scriptFile = helper.setupScriptFile(function);
		String language = helper.getScriptLanguage(function);
		if(language.equals("javascript")) {
			helper.createScriptFromTemplate(scriptFile, "custom_script.js");
		} else if(language.equals("groovy")) {
			helper.createScriptFromTemplate(scriptFile, "custom_script.groovy");
		}
	}

	@Override
	public ScriptFunction newFunction() {
		ScriptFunction function = new ScriptFunction();
		function.getScriptLanguage().setValue("javascript");
		return function;
	}
}
