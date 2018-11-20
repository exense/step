package step.functions.handler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.contextbuilder.ApplicationContextBuilder.ApplicationContext;
import step.grid.contextbuilder.ApplicationContextBuilderException;
import step.grid.contextbuilder.LocalResourceApplicationContextFactory;
import step.grid.contextbuilder.RemoteApplicationContextFactory;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.filemanager.FileProviderException;

public abstract class AbstractFunctionHandler<IN, OUT> {

	private AgentTokenWrapper token;
	private FileManagerClient fileManagerClient;
	private ApplicationContextBuilder applicationContextBuilder;
	private FunctionHandlerFactory functionHandlerFactory;

	public void initialize(AgentTokenWrapper token) {
		this.token = token;
		applicationContextBuilder = token.getServices().getApplicationContextBuilder();
		functionHandlerFactory = new FunctionHandlerFactory();
		fileManagerClient = token.getServices().getFileManagerClient();
	}
	
	/**
	 * @return the current {@link ApplicationContext} with the associate {@link ClassLoader}
	 */
	protected ApplicationContext getCurrentContext() {
		return applicationContextBuilder.getCurrentContext();
	}

	/**
	 * Executes the callable using the {@link ClassLoader} associated to the current {@link ApplicationContext} as context classloader
	 * 
	 * @param callable the callable to be executed in the current {@link ApplicationContext}
	 * @return the result of the callable
	 * @throws Exception
	 */
	protected <T> T runInContext(Callable<T> callable) throws Exception {
		return applicationContextBuilder.runInContext(callable);
	}

	protected void pushLocalApplicationContext(ClassLoader classLoader, String resourceName) throws ApplicationContextBuilderException {
		LocalResourceApplicationContextFactory localContext = new LocalResourceApplicationContextFactory(classLoader, resourceName);
		applicationContextBuilder.pushContext(localContext);			
	}
	
	protected void pushRemoteApplicationContext(String fileId, Map<String, String> properties) throws ApplicationContextBuilderException {
		FileVersionId librariesFileVersion = getFileVersionId(fileId, properties);
		if(librariesFileVersion!=null) {
			RemoteApplicationContextFactory librariesContext = new RemoteApplicationContextFactory(fileManagerClient, librariesFileVersion);
			applicationContextBuilder.pushContext(librariesContext);			
		}
	}
	
	protected File retrieveFileVersion(String properyName, Map<String,String> properties) throws FileProviderException {
		FileVersionId fileVersionId = getFileVersionId(properyName, properties);
		if(fileVersionId != null) {
			return fileManagerClient.requestFile(fileVersionId.getFileId(), fileVersionId.getVersion());
		} else {
			return null;
		}
	}
	
	protected FileVersionId getFileVersionId(String properyName, Map<String,String> properties) {
		String key = properyName+".id";
		if(properties.containsKey(key)) {
			String transferFileId = properties.get(key);
			long transferFileVersion = Long.parseLong(properties.get(properyName+".version"));
			return new FileVersionId(transferFileId, transferFileVersion);			
		} else {
			return null;
		}
	}

	protected abstract Output<OUT> handle(Input<IN> input) throws Exception;
	
	protected Output<OUT> delegate(Class<?> functionHandlerClass, Input<IN> input) throws Exception {
		return delegate(functionHandlerClass.getName(), input);
	}
	
	protected Output<OUT> delegate(String functionHandlerClassname, Input<IN> input) throws Exception {
		return applicationContextBuilder.runInContext(()->{
			@SuppressWarnings("unchecked")
			AbstractFunctionHandler<IN, OUT> functionHandler = functionHandlerFactory.create(token, functionHandlerClassname);
			return functionHandler.handle(input);
		});
	}
	
	protected Map<String, String> mergeAllProperties(Input<?> input) {
		Map<String, String> properties = new HashMap<>();
		if(input.getProperties() != null) {
			properties.putAll(input.getProperties());
		}
		if(token.getProperties()!=null) {
			properties.putAll(token.getProperties());			
		}
		return properties;
	}

	protected AgentTokenWrapper getToken() {
		return token;
	}
	
	public abstract Class<IN> getInputPayloadClass();
	
	public abstract Class<OUT> getOutputPayloadClass();
}
