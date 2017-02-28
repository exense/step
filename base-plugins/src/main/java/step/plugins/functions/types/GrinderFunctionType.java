package step.plugins.functions.types;

import java.util.Map;

import step.commons.conf.Configuration;
import step.functions.type.AbstractFunctionType;

public class GrinderFunctionType extends AbstractFunctionType<GrinderFunction> {

	ScriptFunctionTypeHelper helper;
	
	@Override
	public void init() {
		super.init();
		helper = new ScriptFunctionTypeHelper(getContext());
	}
	
	@Override
	public String getHandlerChain(GrinderFunction function) {
		String jythonLibPath = Configuration.getInstance().getProperty("keywords.grinder.libs.jython.path");
		String grinderLibPath = Configuration.getInstance().getProperty("keywords.grinder.libs.grinder.path");
		return "classuri:"+jythonLibPath+"|classuri:"+grinderLibPath+"|class:step.handlers.scripthandler.ScriptHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(GrinderFunction function) {
		return helper.getHandlerProperties(function);
	}

	@Override
	public GrinderFunction newFunction() {
		return new GrinderFunction();
	}
}
