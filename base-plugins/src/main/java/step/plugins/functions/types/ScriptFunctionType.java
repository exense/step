package step.plugins.functions.types;

import java.io.File;
import java.util.Map;

import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;

@FunctionType(name="script",label="Custom Script")
public class ScriptFunctionType extends AbstractFunctionType<ScriptFunctionTypeConf> {
	
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
	public ScriptFunctionTypeConf newFunctionTypeConf() {
		ScriptFunctionTypeConf conf = new ScriptFunctionTypeConf();
		conf.setCallTimeout(180000);
		conf.setScriptLanguage("javascript");
		return conf;
	}

	@Override
	public void setupFunction(Function function) {
		File scriptFile = helper.setupScriptFile(function);
		String language = helper.getScriptLanguage(function);
		if(language.equals("javascript")) {
			helper.createScriptFromTemplate(scriptFile, "custom_script.js");
		} else if(language.equals("groovy")) {
			helper.createScriptFromTemplate(scriptFile, "custom_script.groovy");
		}
	}

}
