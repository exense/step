package step.plugins.functions.types;

import java.util.Map;

import step.commons.conf.Configuration;
import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;

@FunctionType(name="grinder",label="Grinder")
public class GrinderFunctionType extends AbstractFunctionType<GrinderFunctionTypeConf> {

	ScriptFunctionTypeHelper helper;
	
	@Override
	public void init() {
		super.init();
		helper = new ScriptFunctionTypeHelper(getContext());
	}
	
	@Override
	public String getHandlerChain(Function function) {
		String jythonLibPath = Configuration.getInstance().getProperty("keywords.grinder.libs.jython.path");
		String grinderLibPath = Configuration.getInstance().getProperty("keywords.grinder.libs.grinder.path");
		return "classuri:"+jythonLibPath+"|classuri:"+grinderLibPath+"|class:step.handlers.scripthandler.ScriptHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(Function function) {
		return helper.getHandlerProperties(function);
	}

	@Override
	public GrinderFunctionTypeConf newFunctionTypeConf() {
		GrinderFunctionTypeConf conf = new GrinderFunctionTypeConf();		
		conf.setCallTimeout(180000);
		conf.setScriptLanguage("python");
		return conf;
	}

}
