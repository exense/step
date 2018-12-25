package step.functions.handler;

import java.util.Map;

import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.filemanager.FileManagerClient;

public class FunctionHandlerFactory {
	
	private final ApplicationContextBuilder applicationContextBuilder;
	
	private final FileManagerClient fileManagerClient;
	
	private final Map<String, String> properties;
	
	public FunctionHandlerFactory(ApplicationContextBuilder applicationContextBuilder, FileManagerClient fileManagerClient, Map<String, String> properties) {
		super();
		this.applicationContextBuilder = applicationContextBuilder;
		this.fileManagerClient = fileManagerClient;
		this.properties = properties;
	}

	public AbstractFunctionHandler create(ClassLoader classloader, String class_, TokenSession tokenSession, TokenReservationSession tokenReservationSession) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		AbstractFunctionHandler functionHandler = (AbstractFunctionHandler) classloader.loadClass(class_).newInstance();
		initialize(functionHandler, tokenSession, tokenReservationSession);
		return functionHandler;
	}
	
	public AbstractFunctionHandler initialize(AbstractFunctionHandler functionHandler, TokenSession tokenSession, TokenReservationSession tokenReservationSession) {
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
