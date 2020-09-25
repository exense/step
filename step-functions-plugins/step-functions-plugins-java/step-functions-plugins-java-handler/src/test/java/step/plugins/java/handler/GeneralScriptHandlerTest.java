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
package step.plugins.java.handler;

import java.io.File;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

import junit.framework.Assert;
import step.functions.handler.FunctionHandlerFactory;
import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.filemanager.FileManagerClient;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;
import step.plugins.js223.handler.ScriptHandler;

public class GeneralScriptHandlerTest {

	@Test
	public void testJava() throws Exception {
		GeneralScriptHandler handler = createHandler();
		
		Input<JsonObject> input = new Input<>();
		input.setFunction("MyKeyword1");

		HashMap<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_LANGUAGE, "java");
		properties.put(ScriptHandler.SCRIPT_FILE+".id", "java-plugin-handler-test.jar");
		properties.put(ScriptHandler.SCRIPT_FILE+".version", "");
		input.setProperties(properties);
		
		Output<JsonObject> output = handler.handle(input);
		Assert.assertEquals("MyValue", output.getPayload().getString("MyKey"));
	}
	
	
	@Test
	public void testJS223NashornHappyPath() throws Exception {
		GeneralScriptHandler handler = createHandler();

		Input<JsonObject> input = new Input<>();
		input.setFunction("MyKeyword1");
		input.setPayload(Json.createObjectBuilder().add("key1", "MyValue").build());

		HashMap<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_LANGUAGE, "javascript");
		properties.put(ScriptHandler.SCRIPT_FILE+".id", "scripts/test1.js");
		properties.put(ScriptHandler.SCRIPT_FILE+".version", "");
		input.setProperties(properties);
		
		Output<JsonObject> output = handler.handle(input);
		Assert.assertEquals("MyValue", output.getPayload().getString("key1"));
	}
	
	@Test
	public void testJS223GroovyHappyPath() throws Exception {
		GeneralScriptHandler handler = createHandler();

		Input<JsonObject> input = new Input<>();
		input.setFunction("MyKeyword1");
		input.setPayload(Json.createObjectBuilder().add("key1", "MyValue").build());

		HashMap<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_LANGUAGE, "groovy");
		properties.put(ScriptHandler.SCRIPT_FILE+".id", "scripts/testGroovyUTF8.groovy");
		properties.put(ScriptHandler.SCRIPT_FILE+".version", "");
		input.setProperties(properties);
		
		Output<JsonObject> output = handler.handle(input);
		Assert.assertEquals("kéÿ1", output.getPayload().getString("key1"));
	}
	
	@Test
	public void testJS223GroovyErrorWithoutHandler() throws Exception {
		GeneralScriptHandler handler = createHandler();

		Input<JsonObject> input = new Input<>();
		input.setFunction("MyKeyword1");
		input.setPayload(Json.createObjectBuilder().add("key1", "MyValue").build());

		HashMap<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_LANGUAGE, "groovy");
		properties.put(ScriptHandler.SCRIPT_FILE+".id", "scripts/throwable.groovy");
		properties.put(ScriptHandler.SCRIPT_FILE+".version", "");
		input.setProperties(properties);
		
		Output<JsonObject> output = handler.handle(input);
		Assert.assertEquals("Error while running script throwable.groovy: assert false\n", output.getError().getMsg());
	}
	
	@Test
	public void testJS223GroovyErrorWithHandler() throws Exception {
		GeneralScriptHandler handler = createHandler();

		Input<JsonObject> input = new Input<>();
		input.setFunction("MyKeyword1");
		input.setPayload(Json.createObjectBuilder().add("key1", "MyValue").build());

		HashMap<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_LANGUAGE, "groovy");
		properties.put(ScriptHandler.SCRIPT_FILE+".id", "scripts/throwable.groovy");
		properties.put(ScriptHandler.SCRIPT_FILE+".version", "");
		properties.put(ScriptHandler.ERROR_HANDLER_FILE+".id", "scripts/errorHandler.groovy");
		properties.put(ScriptHandler.ERROR_HANDLER_FILE+".version", "");
		input.setProperties(properties);
		
		Output<JsonObject> output = handler.handle(input);
		Assert.assertEquals("Error handler called", output.getError().getMsg());
	}
	
	@Test
	public void testJS223GroovyErrorWithFailingHandler() throws Exception {
		GeneralScriptHandler handler = createHandler();

		Input<JsonObject> input = new Input<>();
		input.setFunction("MyKeyword1");
		input.setPayload(Json.createObjectBuilder().add("key1", "MyValue").build());

		HashMap<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_LANGUAGE, "groovy");
		properties.put(ScriptHandler.SCRIPT_FILE+".id", "scripts/throwable.groovy");
		properties.put(ScriptHandler.SCRIPT_FILE+".version", "");
		properties.put(ScriptHandler.ERROR_HANDLER_FILE+".id", "scripts/throwable.groovy");
		properties.put(ScriptHandler.ERROR_HANDLER_FILE+".version", "");
		input.setProperties(properties);
		
		Output<JsonObject> output = handler.handle(input);
		Assert.assertEquals("Error while running error handler script: throwable.groovy. assert false\n", output.getError().getMsg());
	}
	
	@Test
	public void testUnknownScriptLanguage() throws Exception {
		GeneralScriptHandler handler = createHandler();

		Input<JsonObject> input = new Input<>();
		input.setFunction("MyKeyword1");
		input.setPayload(Json.createObjectBuilder().add("key1", "MyValue").build());

		HashMap<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_LANGUAGE, "invalidScriptLanguage");
		properties.put(ScriptHandler.SCRIPT_FILE+".id", "scripts/throwable.groovy");
		properties.put(ScriptHandler.SCRIPT_FILE+".version", "");
		input.setProperties(properties);
		
		Output<JsonObject> output = handler.handle(input);
		Assert.assertEquals("Unsupported script language: invalidScriptLanguage", output.getError().getMsg());
	}

	public GeneralScriptHandler createHandler()
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		FunctionHandlerFactory factory = getFunctionHandlerFactory();
		GeneralScriptHandler handler = (GeneralScriptHandler) factory.create(this.getClass().getClassLoader(), GeneralScriptHandler.class.getName(), new TokenSession(), new TokenReservationSession(), new HashMap<>());
		return handler;
	}

	public FunctionHandlerFactory getFunctionHandlerFactory() {
		ApplicationContextBuilder applicationContextBuilder = new ApplicationContextBuilder();
		applicationContextBuilder.forkCurrentContext(GeneralScriptHandler.FORKED_BRANCH);
		FunctionHandlerFactory factory = new FunctionHandlerFactory(applicationContextBuilder, new FileManagerClient() {
			
			@Override
			public FileVersion requestFileVersion(FileVersionId fileVersionId) throws FileManagerException {
				String file = GeneralScriptHandlerTest.class.getClassLoader().getResource(fileVersionId.getFileId()).getFile();
				return new FileVersion(new File(file), fileVersionId, false);
			}
			
			@Override
			public void removeFileVersionFromCache(FileVersionId fileVersionId) {
			}
		});
		return factory;
	}

}
