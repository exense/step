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
	
	public static final String FORKED_BRANCH = "forkedBranch";

	public void initialize(AgentTokenWrapper token) {
		this.token = token;
		applicationContextBuilder = token.getServices().getApplicationContextBuilder();
		functionHandlerFactory = new FunctionHandlerFactory();
		fileManagerClient = token.getServices().getFileManagerClient();
	}
	
	/**
	 * @return the current {@link ApplicationContext} of the default branch
	 */
	protected ApplicationContext getCurrentContext() {
		return getCurrentContext(ApplicationContextBuilder.MASTER);
	}
	
	
	/**
	 * @param branch
	 * @return the current {@link ApplicationContext} of the branch specified in the argument
	 */
	protected ApplicationContext getCurrentContext(String branch) {
		return applicationContextBuilder.getCurrentContext(branch);
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

	/**
	 * Executes the callable in the current context of the branch specified as argument
	 * 
	 * @param branch the name of the branch to be used for execution
	 * @param callable the callable to be executed in the current {@link ApplicationContext}
	 * @return the result of the callable
	 * @throws Exception
	 */
	protected <T> T runInContext(String branch, Callable<T> callable) throws Exception {
		return applicationContextBuilder.runInContext(callable);
	}

	/**
	 * Push a new context based on a local file (jar or zip) to the default branch. See {@link ApplicationContextBuilder} for details
	 * 
	 * @param classLoader the classloader to be used to search the file
	 * @param resourceName the name of the resource to be pushed
	 * @throws ApplicationContextBuilderException
	 */
	protected void pushLocalApplicationContext(ClassLoader classLoader, String resourceName) throws ApplicationContextBuilderException {
		pushLocalApplicationContext(ApplicationContextBuilder.MASTER, classLoader, resourceName);	
	}

	/**
	 *Push a new context based on a local file (jar or zip) to the branch specified as argument. See {@link ApplicationContextBuilder} for details
	 * 
	 * @param branch the name of the branch on which the context has to be pushed
	 * @param classLoader the classloader to be used to search the file
	 * @param resourceName the name of the resource to be pushed
	 * @throws ApplicationContextBuilderException
	 */
	protected void pushLocalApplicationContext(String branch, ClassLoader classLoader, String resourceName) throws ApplicationContextBuilderException {
		LocalResourceApplicationContextFactory localContext = new LocalResourceApplicationContextFactory(classLoader, resourceName);
		applicationContextBuilder.pushContext(branch, localContext);			
	}
	
	/**
	 * Push a new remote context to the default branch. See {@link ApplicationContextBuilder} for details
	 * 
	 * @param fileId the id of the remote file (jar or folder) to be pushed
	 * @param properties
	 * @throws ApplicationContextBuilderException
	 */
	protected void pushRemoteApplicationContext(String fileId, Map<String, String> properties) throws ApplicationContextBuilderException {
		pushRemoteApplicationContext(ApplicationContextBuilder.MASTER, fileId, properties);
	}

	/**
	 * Push a new remote context to the branch specified as argument. See {@link ApplicationContextBuilder} for details
	 * 
	 * @param branch the name of the branch on which the context has to be pushed
	 * @param fileId the id of the remote file (jar or folder) to be pushed
	 * @param properties
	 * @throws ApplicationContextBuilderException
	 */
	protected void pushRemoteApplicationContext(String branch, String fileId, Map<String, String> properties) throws ApplicationContextBuilderException {
		FileVersionId librariesFileVersion = getFileVersionId(fileId, properties);
		if(librariesFileVersion!=null) {
			RemoteApplicationContextFactory librariesContext = new RemoteApplicationContextFactory(fileManagerClient, librariesFileVersion);
			applicationContextBuilder.pushContext(branch, librariesContext);			
		}
	}
	
	/**
	 * Delegate the execution of the function to the {@link AbstractFunctionHandler} specified 
	 * in the arguments in the context of the specified branch
	 * 
	 * @param branchName
	 * @param functionHandlerClassname
	 * @param input
	 * @return
	 * @throws Exception
	 */
	protected Output<OUT> delegate(String branchName, String functionHandlerClassname, Input<IN> input) throws Exception {
		return applicationContextBuilder.runInContext(branchName, ()->{
			@SuppressWarnings("unchecked")
			AbstractFunctionHandler<IN, OUT> functionHandler = functionHandlerFactory.create(applicationContextBuilder.getCurrentContext(branchName).getClassLoader(), functionHandlerClassname, token);
			return functionHandler.handle(input);
		});
	}
	
	/**
	 * Delegate the execution of the function to the {@link AbstractFunctionHandler} specified 
	 * in the arguments in the context of the default branch
	 * 
	 * @param functionHandlerClassname
	 * @param input
	 * @return
	 * @throws Exception
	 */
	protected Output<OUT> delegate(String functionHandlerClassname, Input<IN> input) throws Exception {
		return delegate(ApplicationContextBuilder.MASTER, functionHandlerClassname, input);
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
