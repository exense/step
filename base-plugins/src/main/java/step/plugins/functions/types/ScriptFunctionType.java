package step.plugins.functions.types;

import java.util.HashMap;
import java.util.Map;

import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;

@FunctionType(name="script",label="Script")
public class ScriptFunctionType extends AbstractFunctionType<ScriptFunctionTypeConf> {

	@Override
	public String getHandlerChain(ScriptFunctionTypeConf functionTypeConf) {
		return "class:step.handlers.scripthandler.ScriptHandler";
	}

	@Override
	public Map<String, String> getHandlerProperties(ScriptFunctionTypeConf functionTypeConf) {
		Map<String, String> props = new HashMap<>();
		props.put("scripthandler.script.dir", functionTypeConf.scriptDir);
		return props;
	}

	@Override
	public ScriptFunctionTypeConf newFunctionTypeConf() {
		ScriptFunctionTypeConf conf = new ScriptFunctionTypeConf();
		conf.setCallTimeout(180000);
		return conf;
	}

}
