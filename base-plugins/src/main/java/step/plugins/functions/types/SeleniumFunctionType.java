package step.plugins.functions.types;

import java.io.File;
import java.util.Map;

import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;

@FunctionType(name="selenium",label="Selenium")
public class SeleniumFunctionType extends AbstractFunctionType<SeleniumFunctionTypeConf> {

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
	public SeleniumFunctionTypeConf newFunctionTypeConf() {
		SeleniumFunctionTypeConf conf = new SeleniumFunctionTypeConf();		
		conf.setCallTimeout(180000);
		conf.setScriptLanguage("javascript");
		return conf;
	}

	@Override
	public void setupFunction(Function function) {
		File scriptFile = helper.setupScriptFile(function);
		helper.createScriptFromTemplate(scriptFile, "kw_selenium.js");
	}

}
