package step.plugins.functions.types;

import java.util.HashMap;
import java.util.Map;

import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;

@FunctionType(name="grinder",label="Grinder")
public class GrinderFunctionType extends AbstractFunctionType<GrinderFunctionTypeConf> {

	@Override
	public String getHandlerChain(GrinderFunctionTypeConf functionTypeConf) {
		return "classuri:"+functionTypeConf.jythonLibPath+"|classuri:"+functionTypeConf.grinderLibPath+"|class:step.handlers.scripthandler.ScriptHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(GrinderFunctionTypeConf functionTypeConf) {
		Map<String, String> map = new HashMap<>();
		map.put("scripthandler.script.dir", functionTypeConf.scriptDir);
		return map;
	}

	@Override
	public GrinderFunctionTypeConf newFunctionTypeConf() {
		GrinderFunctionTypeConf conf = new GrinderFunctionTypeConf();
		conf.setGrinderLibPath("../ext/lib/grinder");
		conf.setJythonLibPath("../ext/lib/jython");
		conf.setScriptDir("../data/scripts");
		return conf;
	}

}
