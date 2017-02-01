package step.plugins.functions.types;

import java.io.File;
import java.util.Map;

import org.json.JSONObject;

import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;
import step.functions.type.SetupFunctionException;

@FunctionType(name="selenium",label="Selenium")
public class SeleniumFunctionType extends AbstractFunctionType {

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
		helper.createScriptFromTemplate(scriptFile, "kw_selenium.js");
	}
	
	@Override
	public JSONObject newFunctionTypeConf() {
		JSONObject conf = super.newFunctionTypeConf();
		conf.put("scriptLanguage", "javascript");
		return conf;
	}

}
