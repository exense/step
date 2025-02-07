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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.functions.io.Input;
import step.grid.Token;
import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.MessageHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.filemanager.*;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

import static org.junit.Assert.assertEquals;

public class FunctionMessageHandlerTest {

	private static final Logger logger = LoggerFactory.getLogger(FunctionMessageHandlerTest.class);

	public static final String EMPTY_FILE = "emptyFile";

	public static final String HANDLER_EMPTY_FILE = "handlerEmptyFile";

	protected AgentTokenServices tokenServices;
	
	protected MessageHandlerPool messageHandlerPool;
	
	protected AgentTokenWrapper agentToken;

	protected int expectedFilesInCache = 0;
	
	@Before
	public void before() {
		tokenServices = getLocalAgentTokenServices();
		messageHandlerPool = new MessageHandlerPool(tokenServices);
	}

	@After
	public void after() throws Exception {
		messageHandlerPool.close();
		//The cleanup task of application context is only triggered when the pool of handler and related application context builders are closed
		FunctionMessageHandlerTest.TestFileManagerClient fileManagerClient = (FunctionMessageHandlerTest.TestFileManagerClient) tokenServices.getFileManagerClient();
		assertEquals(expectedFilesInCache, fileManagerClient.cacheUsage.keySet().size());
		fileManagerClient.cacheUsage.forEach((k, v) -> {
			logger.info("Cache usage for {} is {}", k, v);
			assertEquals(0, v.get());
		});
	}

	@Test
	public void test() throws Exception {
		AgentTokenWrapper agentToken = getAgentToken(tokenServices);
		try (TokenReservationSession tokenReservationSession = new TokenReservationSession()) {
			agentToken.setTokenReservationSession(tokenReservationSession);

			InputMessage message = new InputMessage();

			HashMap<String, String> properties = new HashMap<String, String>();

			properties.put(FunctionMessageHandler.FUNCTION_HANDLER_PACKAGE_KEY + ".id", HANDLER_EMPTY_FILE);
			properties.put(FunctionMessageHandler.FUNCTION_HANDLER_PACKAGE_KEY + ".version", "1");

			properties.put(FunctionMessageHandler.FUNCTION_HANDLER_KEY, TestFunctionHandler.class.getName());

			message.setProperties(properties);

			Input<TestInput> input = getTestInput();

			message.setPayload(new ObjectMapper().valueToTree(input));

			OutputMessage outputMessage = messageHandlerPool.get(FunctionMessageHandler.class.getName()).handle(agentToken, message);
			assertEquals("Bonjour", outputMessage.getPayload().get("payload").get("message").asText());
		}
		expectedFilesInCache = 2;

	}

	private Input<TestInput> getTestInput() {
		Input<TestInput> input = new Input<>();
		Map<String, String> inputProperties = new HashMap<>();
		inputProperties.put("myInputProp1", "myInputPropValue1");		
		input.setProperties(inputProperties);
		TestInput testInput = new TestInput();
		testInput.setMessage("Hallo");
		input.setPayload(testInput);;
		return input;
	}
	
	/**
	 * Test the {@link FunctionMessageHandler} in parallel
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testParallel() throws InterruptedException {
		logger.info("Starting FunctionMessageHandler parallel test");
		List<Exception> exceptions = new ArrayList<>();
		int nThreads = 5;
		ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(nThreads);
		for(int i=0;i<nThreads;i++) {
			newFixedThreadPool.submit(()->{
				for(int j=0;j<10;j++) {
					try {
						// reset the context. this is normally performed by the BootstrapManager
						tokenServices.getApplicationContextBuilder().resetContext();
						test();
					} catch (Exception e) {
						exceptions.add(e);
					}
				}
			});
		}
		newFixedThreadPool.shutdown();
		newFixedThreadPool.awaitTermination(10, TimeUnit.SECONDS);

		for (Exception exception : exceptions) {
			exception.printStackTrace();
		}
		assertEquals(0, exceptions.size());
		expectedFilesInCache = 2;
		logger.info("Ending FunctionMessageHandler parallel test");
	}
	
	/**
	 * Test the {@link FunctionMessageHandler} without handler package set in the message properties
	 * @throws Exception
	 */
	@Test
	public void testNoHandlerPackage() throws Exception {
		AgentTokenServices tokenServices = getLocalAgentTokenServices();
		AgentTokenWrapper agentToken = getAgentToken(tokenServices);
		try (TokenReservationSession tokenReservationSession = new TokenReservationSession()) {
			agentToken.setTokenReservationSession(tokenReservationSession);
			FunctionMessageHandler h = new FunctionMessageHandler();
			h.init(tokenServices);

			InputMessage message = new InputMessage();

			HashMap<String, String> properties = new HashMap<String, String>();
			properties.put(FunctionMessageHandler.FUNCTION_HANDLER_KEY, TestFunctionHandler.class.getName());
			message.setProperties(properties);

			Input<TestInput> input = getTestInput();
			message.setPayload(new ObjectMapper().valueToTree(input));

			h.handle(agentToken, message);
		}
	}

	private AgentTokenWrapper getAgentToken(AgentTokenServices tokenServices) {
		Token token = new Token();
		AgentTokenWrapper agentToken = new AgentTokenWrapper(token);
		
		Map<String, String> tokenProperties = new HashMap<>();
		tokenProperties.put("myTokenProp1", "myTokenPropValue1");
		agentToken.setProperties(tokenProperties);
		
		agentToken.setServices(tokenServices);
		return agentToken;
	}

	public class TestFileManagerClient implements FileManagerClient {
		Map<String, AtomicInteger> cacheUsage = new ConcurrentHashMap<>();

		@Override
		public FileVersion requestFileVersion(FileVersionId fileVersionId, boolean cleanable) throws FileManagerException {
			logger.info("Attempting to request FileVersion in TestFileManagerClient for {}", fileVersionId);
			if(fileVersionId.getFileId().equals(EMPTY_FILE) || fileVersionId.getFileId().equals(HANDLER_EMPTY_FILE)) {
				String uid = fileVersionId.getFileId();
				File file = new File(".");
				int i = cacheUsage.computeIfAbsent(fileVersionId.toString(), (k) -> new AtomicInteger(0)).incrementAndGet();
				logger.info("requestFileVersion in TestFileManagerClient for {}, new usage: {}", fileVersionId, i);
				return new FileVersion(file, fileVersionId, false);
			} else {
				logger.error("requested file is null");
				return null;
			}
		}

		@Override
		public void removeFileVersionFromCache(FileVersionId fileVersionId) {
		}

		@Override
		public void cleanupCache() {
		}

		@Override
		public void releaseFileVersion(FileVersion fileVersion) {
			int i = cacheUsage.get(fileVersion.getVersionId().toString()).decrementAndGet();
			logger.info("releaseFileVersion in TestFileManagerClient for {}, new usage: {}", fileVersion.getVersionId(), i);
		}

		@Override
		public void close() throws Exception {
			System.out.println("closing filemanager");
		}
	}

	private AgentTokenServices getLocalAgentTokenServices() {
		AgentTokenServices tokenServices = new AgentTokenServices(new FunctionMessageHandlerTest.TestFileManagerClient());
		tokenServices.setApplicationContextBuilder(new ApplicationContextBuilder());
		Map<String, String> agentProperties = new HashMap<>();
		agentProperties.put("myAgentProp1", "myAgentPropValue1");
		agentProperties.put("myTokenProp1", "defaultValue");
		
		tokenServices.setAgentProperties(agentProperties);
		return tokenServices;
	}

}
