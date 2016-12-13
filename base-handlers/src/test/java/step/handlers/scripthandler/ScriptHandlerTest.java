/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.handlers.scripthandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import junit.framework.Assert;
import step.grid.agent.handler.FunctionTester;
import step.grid.agent.handler.FunctionTester.Context;
import step.grid.io.OutputMessage;

public class ScriptHandlerTest {

	@Test 
	public void test1() {
		String scriptDir = this.getClass().getClassLoader().getResource("scripts").getFile();
		
		Map<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_DIR, scriptDir);
		
		Context context = FunctionTester.getContext(new ScriptHandler(), properties);
		OutputMessage out = context.run("test1", "{\"key1\":\"val1\"}");
		
		Assert.assertEquals("val1",out.getPayload().getString("key1"));
	}

	@Test 
	public void testGroovy1() {
		String scriptDir = this.getClass().getClassLoader().getResource("scripts").getFile();
		
		Map<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_DIR, scriptDir);
		
		Context context = FunctionTester.getContext(new ScriptHandler(), properties);
		OutputMessage out = context.run("testGroovy1", "{\"key1\":\"val1\"}");
		
		Assert.assertEquals("val1",out.getPayload().getString("key1"));
	}
	

	@Test 
	public void testPython1() {
		String scriptDir = this.getClass().getClassLoader().getResource("scripts").getFile();
		
		Map<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_DIR, scriptDir);
		
		Context context = FunctionTester.getContext(new ScriptHandler(), properties);
		OutputMessage out = context.run("testPython", "{\"key1\":\"val1\"}");
		
		Assert.assertEquals("val1",out.getPayload().getString("key1"));
	}
	
	@Test 
	public void testParallel() throws InterruptedException, ExecutionException, TimeoutException {
		Map<String, String> properties = new HashMap<>();
		String scriptDir = this.getClass().getClassLoader().getResource("scripts").getFile();
		properties.put(ScriptHandler.SCRIPT_DIR, scriptDir);

		final ScriptHandler handler = new ScriptHandler();
		
		int nIt = 1000;
		int nThreads = 10;
		ExecutorService e = Executors.newFixedThreadPool(nThreads);
		
		List<Future<Boolean>> results = new ArrayList<>();
		
		for(int i=0;i<nIt;i++) {
			results.add(e.submit(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					Context context = FunctionTester.getContext(handler, properties);
					OutputMessage out = context.run("test1", "{\"key1\":\"val1\"}");
					
					Assert.assertEquals("val1",out.getPayload().getString("key1"));
					return true;
				}
			}));
		}
		
		for (Future<Boolean> future : results) {
			future.get(1, TimeUnit.MINUTES);
		}
	}
	
	@Test 
	public void testWrongScriptEngine() {
		String scriptDir = this.getClass().getClassLoader().getResource("scripts").getFile();
		
		Map<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_DIR, scriptDir);
		
		Context context = FunctionTester.getContext(new ScriptHandler(), properties);
		try {
			context.run("noextension", "{\"key1\":\"val1\"}");
		} catch(Exception e) {
			Assert.assertTrue(e.getMessage().contains("The file 'noextension' has no extension. Please add one of the following extensions:"));			
		}		
	}
	
	@Test 
	public void testGroovy() {
		String scriptDir = this.getClass().getClassLoader().getResource("scripts").getFile();
		
		Map<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_DIR, scriptDir);
		
		Context context = FunctionTester.getContext(new ScriptHandler(), properties);
		OutputMessage out = context.run("testGroovyUTF8", "{\"key1\":\"val1\"}");
		Assert.assertEquals("kéÿ1",out.getPayload().getString("key1"));
	}
	
	@Test 
	public void testErrorHandler() {
		String scriptDir = this.getClass().getClassLoader().getResource("scripts").getFile();
		
		Map<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_DIR, scriptDir);
		properties.put(ScriptHandler.ERROR_HANDLER_SCRIPT, scriptDir + "/errorHandler.js");
		
		Context context = FunctionTester.getContext(new ScriptHandler(), properties);
		try {
			OutputMessage out = context.run("errorScript", "{\"key1\":\"val1\"}");
			Assert.assertFalse(true);
		} catch(Exception e) {
			Assert.assertEquals("executed", System.getProperties().get("errorHandler"));
		}
	}
}
