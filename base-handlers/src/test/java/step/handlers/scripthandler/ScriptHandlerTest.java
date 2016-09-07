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
	public void testGroovy() {
		String scriptDir = this.getClass().getClassLoader().getResource("scripts").getFile();
		
		Map<String, String> properties = new HashMap<>();
		properties.put(ScriptHandler.SCRIPT_DIR, scriptDir);
		properties.put(ScriptHandler.SCRIPT_ENGINE, "dummy");
		
		Context context = FunctionTester.getContext(new ScriptHandler(), properties);
		try {
			context.run("testGroovy", "{\"key1\":\"val1\"}");
		} catch(Exception e) {
			Assert.assertTrue(e.getMessage().contains("Unable to find script engine with name 'dummy'"));			
		}		
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
