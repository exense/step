package step.functions.handler;

import java.util.Map;

import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.filemanager.FileManagerClient;

public class FunctionHandlerFactory {
	
	private final ApplicationContextBuilder applicationContextBuilder;
	
	private final FileManagerClient fileManagerClient;
	
	public FunctionHandlerFactory(ApplicationContextBuilder applicationContextBuilder, FileManagerClient fileManagerClient) {
		super();
		this.applicationContextBuilder = applicationContextBuilder;
		this.fileManagerClient = fileManagerClient;
	}

	/**
	 * Creates a new instance of {@link AbstractFunctionHandler}
	 * @param classloader the {@link ClassLoader} to be used to load the specified class
	 * @param class_ the class to be instantiated
	 * @param tokenSession the {@link TokenSession} to be injected to the {@link AbstractFunctionHandler}
	 * @param tokenReservationSession the {@link TokenReservationSession} to be injected to the {@link AbstractFunctionHandler}
	 * @param properties the properties to be injected to the {@link AbstractFunctionHandler}
	 * @return the created instance of  {@link AbstractFunctionHandler}
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("rawtypes")
	public AbstractFunctionHandler create(ClassLoader classloader, String class_, TokenSession tokenSession, TokenReservationSession tokenReservationSession, Map<String, String> properties) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		AbstractFunctionHandler functionHandler = (AbstractFunctionHandler) classloader.loadClass(class_).newInstance();
		initialize(functionHandler, tokenSession, tokenReservationSession, properties);
		return functionHandler;
	}
	
	/**
	 * Initializes an instance of {@link AbstractFunctionHandler}
	 * @param functionHandler the instance to be initialized
	 * @param tokenSession the {@link TokenSession} to be injected to the {@link AbstractFunctionHandler}
	 * @param tokenReservationSession the {@link TokenReservationSession} to be injected to the {@link AbstractFunctionHandler}
	 * @param properties the properties to be injected to the {@link AbstractFunctionHandler}
	 * @return the initialized {@link AbstractFunctionHandler}
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public AbstractFunctionHandler initialize(AbstractFunctionHandler functionHandler, TokenSession tokenSession, TokenReservationSession tokenReservationSession, Map<String, String> properties) {
		functionHandler.setFunctionHandlerFactory(this);
		functionHandler.setApplicationContextBuilder(applicationContextBuilder);
		functionHandler.setFileManagerClient(fileManagerClient);
		functionHandler.setProperties(properties);
		
		functionHandler.setTokenSession(tokenSession);
		functionHandler.setTokenReservationSession(tokenReservationSession);
		
		functionHandler.initialize();
		return functionHandler;
	}
}
