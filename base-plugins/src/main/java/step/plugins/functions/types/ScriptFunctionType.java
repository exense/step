package step.plugins.functions.types;

import java.io.File;
import java.util.Map;

import org.json.JSONObject;

import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;
import step.functions.type.SetupFunctionException;

@FunctionType(name="script",label="Custom Script (JS, Groovy, etc)")
public class ScriptFunctionType extends AbstractFunctionType {
	
	ScriptFunctionTypeHelper helper;
	
	@Override
	public void init() {
		super.init();
		helper = new ScriptFunctionTypeHelper(getContext());
	}

	@Override
	public String getHandlerChain(Function function) {
		return "class:step.handlers.scripthandler.ScriptHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(Function function) {
		return helper.getHandlerProperties(function);
	}

	@Override
	public void setupFunction(Function function) throws SetupFunctionException {
		File scriptFile = helper.setupScriptFile(function);
		String language = helper.getScriptLanguage(function);
		if(language.equals("javascript")) {
			helper.createScriptFromTemplate(scriptFile, "custom_script.js");
		} else if(language.equals("groovy")) {
			helper.createScriptFromTemplate(scriptFile, "custom_script.groovy");
		}
	}

	@Override
	public JSONObject newFunctionTypeConf() {
		JSONObject conf = super.newFunctionTypeConf();
		conf.put("scriptLanguage", "javascript");
		return conf;
	}
}
