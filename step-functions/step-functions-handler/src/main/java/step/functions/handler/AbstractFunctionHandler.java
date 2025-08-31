/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.functions.handler;

import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.contextbuilder.*;
import step.grid.contextbuilder.ApplicationContextBuilder.ApplicationContext;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;
import step.reporting.LiveReporting;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class AbstractFunctionHandler<IN, OUT> {

	private FileManagerClient fileManagerClient;
	private LiveReporting liveReporting;
	private ApplicationContextBuilder applicationContextBuilder;
	private FunctionHandlerFactory functionHandlerFactory;
	
	private TokenSession tokenSession;
	private TokenReservationSession tokenReservationSession;
	
	private Map<String, String> properties;
	
	public static final String FORKED_BRANCH = "forkedBranch";
	public static final String PARENTREPORTID_KEY = "$parentreportid";
	public static final String EXECUTION_CONTEXT_KEY = "$executionContext";
	public static final String ARTEFACT_PATH = "$artefactPath";

	protected FunctionHandlerFactory getFunctionHandlerFactory() {
		return functionHandlerFactory;
	}

	protected void setFunctionHandlerFactory(FunctionHandlerFactory functionHandlerFactory) {
		this.functionHandlerFactory = functionHandlerFactory;
	}

	protected void setApplicationContextBuilder(ApplicationContextBuilder applicationContextBuilder) {
		this.applicationContextBuilder = applicationContextBuilder;
	}

	protected void setFileManagerClient(FileManagerClient fileManagerClient) {
		this.fileManagerClient = fileManagerClient;
	}

	void setLiveReporting(LiveReporting liveReporting) {
		this.liveReporting = liveReporting;
	}

	protected LiveReporting getLiveReporting() {
		return liveReporting;
	}

	protected void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	protected Map<String, String> getProperties() {
		return properties;
	}

	protected TokenSession getTokenSession() {
		return tokenSession;
	}

	protected void setTokenSession(TokenSession tokenSession) {
		this.tokenSession = tokenSession;
	}

	protected TokenReservationSession getTokenReservationSession() {
		return tokenReservationSession;
	}

	protected void setTokenReservationSession(TokenReservationSession tokenReservationSession) {
		this.tokenReservationSession = tokenReservationSession;
	}

	protected void registerObjectToBeClosedWithSession(AutoCloseable autoCloseable) {
		getTokenReservationSession().registerObjectToBeClosedWithSession(autoCloseable);
	}

	public void initialize() {
	}
	
	/**
	 * @return the current {@link ApplicationContext} of the default branch
	 */
	protected ApplicationContext getCurrentContext() {
		return getCurrentContext(ApplicationContextBuilder.MASTER);
	}
	
	
	/**
	 * @param branch the name of the branch for which we retrieve the current application context
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
	 * @throws Exception any exception that may occur while executing in the application context
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
	 * @throws Exception any exception that may occur while executing in the application context
	 */
	protected <T> T runInContext(String branch, Callable<T> callable) throws Exception {
		return applicationContextBuilder.runInContext(branch, callable);
	}

	/**
	 * Push a new context based on a local file (jar or zip) to the default branch. See {@link ApplicationContextBuilder} for details
	 * 
	 * @param classLoader the classloader to be used to search the file
	 * @param resourceName the name of the resource to be pushed
	 * @throws ApplicationContextBuilderException exception occurring while building the application context or underlying class loader
	 */
	protected void pushLocalApplicationContext(ClassLoader classLoader, String resourceName) throws ApplicationContextBuilderException {
		pushLocalApplicationContext(ApplicationContextBuilder.MASTER, classLoader, resourceName);
	}

	/**
	 * Push a new context based on a local file (jar or zip) to the branch specified as argument. See {@link ApplicationContextBuilder} for details
	 *
	 * @param branch       the name of the branch on which the context has to be pushed
	 * @param classLoader  the classloader to be used to search the file
	 * @param resourceName the name of the resource to be pushed
	 * @throws ApplicationContextBuilderException exception occurring while building the application context or underlying class loader
	 */
	protected void pushLocalApplicationContext(String branch, ClassLoader classLoader, String resourceName) throws ApplicationContextBuilderException {
		LocalResourceApplicationContextFactory localContext = new LocalResourceApplicationContextFactory(classLoader, resourceName);
		registerObjectToBeClosedWithSession(applicationContextBuilder.pushContext(branch, localContext, true));
	}
	
	/**
	 * Push a new context based on a local folder containing a list of jars to the master branch. See {@link ApplicationContextBuilder} for details
	 *
	 * @param libFolder the folder containing the jars to be pushed to the context
	 * @throws ApplicationContextBuilderException exception occurring while building the application context or underlying class loader
	 */
	protected void pushLocalFolderApplicationContext(File libFolder) throws ApplicationContextBuilderException {
		LocalFolderApplicationContextFactory localContext = new LocalFolderApplicationContextFactory(libFolder);
		registerObjectToBeClosedWithSession(applicationContextBuilder.pushContext(ApplicationContextBuilder.MASTER, localContext, true));
	}
	
	/**
	 * Push a new context based on a local folder containing a list of jars to the branch specified as argument. See {@link ApplicationContextBuilder} for details
	 *
	 * @param branch    the name of the branch on which the context has to be pushed
	 * @param libFolder the folder containing the jars to be pushed to the context
	 * @throws ApplicationContextBuilderException exception occurring while building the application context or underlying class loader
	 */
	protected void pushLocalFolderApplicationContext(String branch, File libFolder) throws ApplicationContextBuilderException {
		LocalFolderApplicationContextFactory localContext = new LocalFolderApplicationContextFactory(libFolder);
		registerObjectToBeClosedWithSession(applicationContextBuilder.pushContext(branch, localContext, true));
	}
	
	/**
	 * Push a new remote context to the default branch. See {@link ApplicationContextBuilder} for details
	 *
	 * @param fileId the id of the remote file (jar or folder) to be pushed
	 * @param properties the map containing the metadata to get the corresponding {@link FileVersionId}
	 * @throws ApplicationContextBuilderException exception occurring while building the application context or underlying class loader
	 */
	protected void pushRemoteApplicationContext(String fileId, Map<String, String> properties) throws ApplicationContextBuilderException {
		pushRemoteApplicationContext(fileId, properties, true);
	}

	/**
	 * Push a new remote context to the default branch. See {@link ApplicationContextBuilder} for details
	 *
	 * @param fileId the id of the remote file (jar or folder) to be pushed
	 * @param properties the map containing the metadata to get the corresponding {@link FileVersionId}
	 * @param cleanable  whether the remote file can be cleaned once stored in the local cache
	 * @throws ApplicationContextBuilderException exception occurring while building the application context or underlying class loader
	 */
	protected void pushRemoteApplicationContext(String fileId, Map<String, String> properties, boolean cleanable) throws ApplicationContextBuilderException {
		pushRemoteApplicationContext(ApplicationContextBuilder.MASTER, fileId, properties, cleanable);
	}

	/**
	 * Push a new remote context to the branch specified as argument. See {@link ApplicationContextBuilder} for details
	 *
	 * @param branch     the name of the branch on which the context has to be pushed
	 * @param fileId     the id of the remote file (jar or folder) to be pushed
	 * @param properties the map containing the metadata to get the corresponding {@link FileVersionId}
	 * @param cleanable  whether the remote file can be cleaned once stored in the local cache
	 * @throws ApplicationContextBuilderException exception occurring while building the application context or underlying class loader
	 */
	protected void pushRemoteApplicationContext(String branch, String fileId, Map<String, String> properties, boolean cleanable) throws ApplicationContextBuilderException {
		FileVersionId librariesFileVersion = getFileVersionId(fileId, properties);
		if(librariesFileVersion!=null) {
			RemoteApplicationContextFactory librariesContext = new RemoteApplicationContextFactory(fileManagerClient, librariesFileVersion, cleanable);
			registerObjectToBeClosedWithSession(applicationContextBuilder.pushContext(branch, librariesContext, cleanable));
		}
	}
	
	/**
	 * Delegate the execution of the function to the {@link AbstractFunctionHandler} specified 
	 * in the arguments in the context of the specified branch
	 * 
	 * @param branchName the name of the application context branch to be used
	 * @param functionHandlerClassname the function handler class name to which we delegate the handling
	 * @param input the input to be passed
	 * @return output of the function execution
	 * @throws Exception any exception thrown during the process
	 */
	protected Output<OUT> delegate(String branchName, String functionHandlerClassname, Input<IN> input) throws Exception {
		return applicationContextBuilder.runInContext(branchName, ()->{
			@SuppressWarnings("unchecked")
			AbstractFunctionHandler<IN, OUT> functionHandler = functionHandlerFactory.create(applicationContextBuilder.getCurrentContext(branchName).getClassLoader(), functionHandlerClassname, tokenSession, tokenReservationSession, properties);
			// TODO: I'm not perfectly happy with this. Ideally the streamingUploads/livereporting and all other "services" should be passed at creation to FunctionHandlerFactory.create
			functionHandler.setLiveReporting(liveReporting);
			return functionHandler.handle(input);
		});
	}
	
	/**
	 * Delegate the execution of the function to the {@link AbstractFunctionHandler} specified 
	 * in the arguments in the context of the default branch
	 * 
	 * @param functionHandlerClassname he function handler class name to which we delegate the handling
	 * @param input the input to be passed to the function
	 * @return output of the function execution
	 * @throws Exception any exception thrown during the process
	 */
	protected Output<OUT> delegate(String functionHandlerClassname, Input<IN> input) throws Exception {
		return delegate(ApplicationContextBuilder.MASTER, functionHandlerClassname, input);
	}

	private class FileVersionCloseable implements AutoCloseable {

		FileVersion fileVersion;

		public FileVersionCloseable(FileVersion fileVersion) {
			this.fileVersion = fileVersion;
		}

		@Override
		public void close() throws Exception {
			if (fileVersion != null) {
				releaseFileVersion(fileVersion);
			}
		}
	}

	protected File retrieveFileVersion(String properyName, Map<String,String> properties, boolean cleanable) throws FileManagerException {
		FileVersionId fileVersionId = getFileVersionId(properyName, properties);
		if(fileVersionId != null) {
			FileVersion fileVersion = fileManagerClient.requestFileVersion(fileVersionId, cleanable);
			if (fileVersion != null) {
				registerObjectToBeClosedWithSession(new FileVersionCloseable(fileVersion));
				return fileVersion.getFile();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	protected File retrieveFileVersion(String properyName, Map<String,String> properties) throws FileManagerException {
		return retrieveFileVersion(properyName,properties, true);
	}

	private void releaseFileVersion(FileVersion fileVersion) {
		fileManagerClient.releaseFileVersion(fileVersion);
	}
	
	protected FileVersionId getFileVersionId(String properyName, Map<String,String> properties) {
		String key = properyName+".id";
		if(properties.containsKey(key)) {
			String transferFileId = properties.get(key);
			String transferFileVersion = properties.get(properyName+".version");
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
		if(this.properties!=null) {
			properties.putAll(this.properties);
		}
		return properties;
	}
	
	public abstract Class<IN> getInputPayloadClass();
	
	public abstract Class<OUT> getOutputPayloadClass();
}
