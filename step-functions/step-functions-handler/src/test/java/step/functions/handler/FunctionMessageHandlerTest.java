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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.Assert;
import step.functions.io.Input;
import step.grid.Token;
import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.MessageHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class FunctionMessageHandlerTest {

	public static final String EMPTY_FILE = "emptyFile";

	protected AgentTokenServices tokenServices;
	
	protected MessageHandlerPool messageHandlerPool;
	
	protected AgentTokenWrapper agentToken;
	
	@Before
	public void before() {
		tokenServices = getLocalAgentTokenServices();
		messageHandlerPool = new MessageHandlerPool(tokenServices);
	}

	@Test
	public void test() throws Exception {
		AgentTokenWrapper agentToken = getAgentToken(tokenServices);
		
		InputMessage message = new InputMessage();
		
		HashMap<String, String> properties = new HashMap<String, String>();

		properties.put(FunctionMessageHandler.FUNCTION_HANDLER_PACKAGE_KEY + ".id", EMPTY_FILE);
		properties.put(FunctionMessageHandler.FUNCTION_HANDLER_PACKAGE_KEY + ".version", "1");
		
		properties.put(FunctionMessageHandler.FUNCTION_HANDLER_KEY, TestFunctionHandler.class.getName());
		
		message.setProperties(properties);
		
		Input<TestInput> input = getTestInput();
		
		message.setPayload(new ObjectMapper().valueToTree(input));
		
		OutputMessage outputMessage = messageHandlerPool.get(FunctionMessageHandler.class.getName()).handle(agentToken, message);
		Assert.assertEquals("Bonjour", outputMessage.getPayload().get("payload").get("message").asText());
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
		Assert.assertEquals(0, exceptions.size());
	}
	
	/**
	 * Test the {@link FunctionMessageHandler} without handler package set in the message properties
	 * @throws Exception
	 */
	@Test
	public void testNoHandlerPackage() throws Exception {
		AgentTokenServices tokenServices = getLocalAgentTokenServices();
		AgentTokenWrapper agentToken = getAgentToken(tokenServices);
		
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

	private AgentTokenWrapper getAgentToken(AgentTokenServices tokenServices) {
		Token token = new Token();
		AgentTokenWrapper agentToken = new AgentTokenWrapper(token);
		
		Map<String, String> tokenProperties = new HashMap<>();
		tokenProperties.put("myTokenProp1", "myTokenPropValue1");
		agentToken.setProperties(tokenProperties);
		
		agentToken.setServices(tokenServices);
		return agentToken;
	}

	private AgentTokenServices getLocalAgentTokenServices() {
		AgentTokenServices tokenServices = new AgentTokenServices(new FileManagerClient() {

			@Override
			public FileVersion requestFileVersion(FileVersionId fileVersionId, boolean cleanable) throws FileManagerException {
				if(fileVersionId.getFileId().equals(EMPTY_FILE)) {
					String uid = fileVersionId.getFileId();
					File file = new File(".");
					return new FileVersion(file, fileVersionId, false);
				} else {
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
			public void close() throws Exception {

			}
		});
		tokenServices.setApplicationContextBuilder(new ApplicationContextBuilder());
		Map<String, String> agentProperties = new HashMap<>();
		agentProperties.put("myAgentProp1", "myAgentPropValue1");
		agentProperties.put("myTokenProp1", "defaultValue");
		
		tokenServices.setAgentProperties(agentProperties);
		return tokenServices;
	}

}
