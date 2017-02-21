package step.functions.type;

import java.io.InputStream;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.json.JSONObject;

import step.core.GlobalContext;
import step.functions.Function;

public abstract class AbstractFunctionType {

	protected GlobalContext context;
	
	public GlobalContext getContext() {
		return context;
	}

	public void setContext(GlobalContext context) {
		this.context = context;
	}
	
	public void init() {}

	public abstract String getHandlerChain(Function function);
	
	public abstract Map<String, String> getHandlerProperties(Function function);
	
	public JSONObject newFunctionTypeConf() {
		JSONObject configuration = new JSONObject();
		configuration.put("callTimeout", 180000);
		return configuration;
	}
	
	private InputStream getLocalResourceAsStream(String resourceName) {
		String typeName = this.getClass().getAnnotation(FunctionType.class).name();
		return this.getClass().getResourceAsStream("/step/plugins/functions/types/"+typeName+"/"+resourceName);
	}
	
	public JsonObject getConfigurationSchema() {
		InputStream in = getLocalResourceAsStream("conf.schema.json");
		return Json.createReader(in).readObject();
	}
	
	public JsonArray getConfigurationForm() {
		InputStream confForm = getLocalResourceAsStream("conf.form.json");
		if(confForm==null) {
			confForm = this.getClass().getResourceAsStream("/step/functions/type/conf.form.default.json");
		}
		return Json.createReader(confForm).readArray();
	}
	
	public void setupFunction(Function function) throws SetupFunctionException {
		
	}
	
	public Function copyFunction(Function function) {
		function.setId(null);
		function.getAttributes().put("name",function.getAttributes().get("name")+"_Copy");
		return function;
	}
}
