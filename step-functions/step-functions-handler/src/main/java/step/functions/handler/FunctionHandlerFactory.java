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
