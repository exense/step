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
import step.commons.helpers.FileHelper;
import step.grid.agent.handler.FunctionTester;
import step.grid.agent.handler.FunctionTester.Context;
import step.grid.io.OutputMessage;

public class ScriptHandlerTest {

	@Test 
	public void test1() {
		Context context = FunctionTester.getContext(new ScriptHandler(), getProperties("test1.js"));
		OutputMessage out = context.run("", "{\"key1\":\"val1\"}");
		
		Assert.assertEquals("val1",out.getPayload().getString("key1"));
	}

	private Map<String, String> getProperties(String filename) {
		Map<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_FILE, getScriptDir() + "/" + filename);
		return properties;
	}

	@Test 
	public void testGroovy1() {
		Context context = FunctionTester.getContext(new ScriptHandler(), getProperties("testGroovy1.groovy"));
		OutputMessage out = context.run("", "{\"key1\":\"val1\"}");
		
		Assert.assertEquals("val1",out.getPayload().getString("key1"));
	}
	

	@Test 
	public void testPython1() {
		Context context = FunctionTester.getContext(new ScriptHandler(), getProperties("testPython.py"));
		OutputMessage out = context.run("", "{\"key1\":\"val1\"}");
		
		Assert.assertEquals("val1",out.getPayload().getString("key1"));
	}
	
	@Test 
	public void testParallel() throws InterruptedException, ExecutionException, TimeoutException {

		final ScriptHandler handler = new ScriptHandler();
		
		int nIt = 100;
		int nThreads = 10;
		ExecutorService e = Executors.newFixedThreadPool(nThreads);
		
		List<Future<Boolean>> results = new ArrayList<>();
		
		for(int i=0;i<nIt;i++) {
			results.add(e.submit(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					Context context = FunctionTester.getContext(handler, getProperties("test1.js"));
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
	public void testGroovy() {
		Context context = FunctionTester.getContext(new ScriptHandler(), getProperties("testGroovyUTF8.groovy"));
		OutputMessage out = context.run("", "{\"key1\":\"val1\"}");
		Assert.assertEquals("kéÿ1",out.getPayload().getString("key1"));
	}
	
	@Test 
	public void testErrorHandler() {
		Map<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_FILE, getScriptDir() + "/errorScript.js");
		properties.put(ScriptHandler.ERROR_HANDLER_FILE, getScriptDir() + "/errorHandler.js");

		Context context = FunctionTester.getContext(new ScriptHandler(), properties);
		try {
			OutputMessage out = context.run("", "{\"key1\":\"val1\"}");
			Assert.assertFalse(true);
		} catch(Exception e) {
			Assert.assertEquals("executed", System.getProperties().get("errorHandler"));
		}
	}
	
	private String getScriptDir() {
		return FileHelper.getClassLoaderResource(this.getClass(),"scripts").getAbsolutePath();
	}
}
