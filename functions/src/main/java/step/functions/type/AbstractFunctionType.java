package step.functions.type;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import step.attachments.FileResolver;
import step.commons.helpers.FileHelper;
import step.core.GlobalContext;
import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;
import step.functions.Input;
import step.functions.FunctionClient;
import step.grid.agent.Agent;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.tokenpool.Interest;
import step.plugins.adaptergrid.GridPlugin;

public abstract class AbstractFunctionType<T extends Function> {

	protected GlobalContext context;
	
	public GlobalContext getContext() {
		return context;
	}

	public void setContext(GlobalContext context) {
		this.context = context;
	}
	
	public void init() {}

	public Map<String, Interest> getTokenSelectionCriteria(T function) {
		Map<String, Interest> criteria = new HashMap<>();
		criteria.put(Agent.AGENT_TYPE_KEY, new Interest(Pattern.compile("default"), true));
		return criteria;
	}
	
	public abstract String getHandlerChain(T function);
	
	public FileVersionId getHandlerPackage(T function) {
		return null;
	};
	
	public abstract Map<String, String> getHandlerProperties(T function);
	
	public void beforeFunctionCall(T function, Input input, Map<String, String> properties) {
		
	}
	
	public abstract T newFunction();
	
	public void setupFunction(T function) throws SetupFunctionException {
		
	}
	
	public T updateFunction(T function) throws FunctionTypeException {
		return function;
	}
	
	public T copyFunction(T function) throws FunctionTypeException {
		function.setId(null);
		function.getAttributes().put(Function.NAME,function.getAttributes().get(Function.NAME)+"_Copy");
		return function;
	}
	
	protected void registerFile(DynamicValue<String> dynamicValue, String properyName, Map<String, String> props) {
		String filepath = dynamicValue.get();
		if(filepath!=null && filepath.trim().length()>0) {
			FileResolver fileResolver = new FileResolver(context.getAttachmentManager());
			
			File file = fileResolver.resolve(filepath);
			registerFile(file, properyName, props);			
		}
	}
	
	protected void registerFile(File file, String properyName, Map<String, String> props) {
		FunctionClient functionClient = (FunctionClient) context.get(GridPlugin.FUNCTIONCLIENT_KEY);
		String fileHandle = functionClient.registerAgentFile(file);
		props.put(properyName+".id", fileHandle);
		props.put(properyName+".version", Long.toString(FileHelper.getLastModificationDateRecursive(file)));
	}
	
	protected FileVersionId registerFile(File file) {
		FunctionClient functionClient = (FunctionClient) context.get(GridPlugin.FUNCTIONCLIENT_KEY);
		String fileHandle = functionClient.registerAgentFile(file);
		return new FileVersionId(fileHandle, FileHelper.getLastModificationDateRecursive(file));
	}
	
	protected FileVersionId registerFile(String filepath) {
		return registerFile(new File(filepath));
	}
	
	public void deleteFunction(T function) throws FunctionTypeException {

	}
}
