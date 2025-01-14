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
package step.plugins.java;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ch.exense.commons.test.categories.PerformanceTest;
import jakarta.json.JsonObject;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import ch.exense.commons.app.Configuration;
import ch.exense.commons.io.FileHelper;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.CallFunction;
import step.artefacts.handlers.functions.TokenForecastingExecutionPlugin;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.execution.OperationMode;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.plugins.FunctionPlugin;
import step.functions.io.Output;
import step.functions.runner.FunctionRunner;
import step.functions.runner.FunctionRunner.Context;
import step.grid.client.MockedGridClientImpl;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.threadpool.ThreadPoolPlugin;

import static org.junit.Assert.*;

public class ScriptHandlerTest {

	private static final Logger logger = LoggerFactory.getLogger(ScriptHandlerTest.class);
	private MockedGridClientImpl gridClient;

	@Test 
	public void test1() {
		GeneralScriptFunction f = buildTestFunction("javascript","test1.js");
		Output<JsonObject> output = run(f, "{\"key1\":\"val1\"}");
		Assert.assertEquals("val1",output.getPayload().getString("key1"));
	}
	
	@Test 
	public void testProperties() {
		
		Map<String, String> tokenProperties = new HashMap<>();
		tokenProperties.put("tokenProp1", "MyTokenProp");
		
		Map<String, String> properties = new HashMap<>();
		properties.put("prop1", "MyProp");
		
		GeneralScriptFunction f = buildTestFunction("javascript","test1.js");
		Output<JsonObject> output = run(f, "{\"key1\":\"val1\"}", properties);
		Assert.assertEquals("val1",output.getPayload().getString("key1"));
		Assert.assertEquals("MyProp",output.getPayload().getString("prop1"));
	}

	@Test
	public void testWithEngine() throws IOException {
		MockedGridClientImpl gridClient = null;
		try (ExecutionEngine executionEngine = ExecutionEngine.builder().withPlugin(new ThreadPoolPlugin())
				.withPlugin(new BaseArtefactPlugin()).withPlugin(new TokenForecastingExecutionPlugin())
				.withPlugin(new FunctionPlugin()).withPlugin(new GeneralScriptFunctionPlugin()).build()) {
			GeneralScriptFunction function = buildTestFunction("javascript", "test1.js");
			function.addAttribute(AbstractOrganizableObject.NAME, "myJavascriptKeyword");
			CallFunction callFunction = FunctionArtefacts.keyword(function.getAttribute(AbstractOrganizableObject.NAME), "{\"key1\":\"val1\"}");
			Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.threadGroup(1, 1)).add(callFunction).endBlock().build();
			plan.setFunctions(List.of(function));

			PlanRunnerResult execute = executionEngine.execute(plan);
			execute.printTree();
			assertEquals(ReportNodeStatus.PASSED, execute.getResult());
			gridClient = (MockedGridClientImpl) executionEngine.getExecutionEngineContext().get("step.grid.client.GridClient");
			assertEquals(3, gridClient.cacheUsage.size());
			gridClient.cacheUsage.values().forEach(v -> assertTrue((!v.cleanable || v.usageCount.get() == 0)));
		}
		assertNotNull(gridClient);
		//Files are all released when the execution engine and underlying AbstractGridClientImpl are closed because we use local token
		// because the local message handler pool and underlying message handlers are only closed here
		assertEquals(3, gridClient.cacheUsage.size());
		gridClient.cacheUsage.values().forEach(v -> assertTrue((v.usageCount.get() == 0 || (v.usageCount.get() == 1 && !v.cleanable) )));
		MockedGridClientImpl.MockedFileManagerClient fileManagerClient = (MockedGridClientImpl.MockedFileManagerClient) gridClient.fileManagerClient;
		assertEquals(2, fileManagerClient.clientCacheUsage.size());
		fileManagerClient.clientCacheUsage.values().forEach(v -> assertTrue((!v.cleanable || v.usageCount.get() == 0)));

	}
	
	private Output<JsonObject> run(GeneralScriptFunction f, String inputJson) {
		return run(f, inputJson, new HashMap<>());
	}
	
	private Output<JsonObject> run(GeneralScriptFunction f, String inputJson, Map<String, String> properties) {
		try (Context context = FunctionRunner.getContext(new GeneralScriptFunctionType(new Configuration()), properties)) {
			return context.run(f, inputJson);			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test 
	public void testGroovy1() {
		GeneralScriptFunction f = buildTestFunction("groovy","testGroovy1.groovy");
		Output<JsonObject> output = run(f, "{\"key1\":\"val1\"}");
		Assert.assertEquals("val1",output.getPayload().getString("key1"));
	}

	@Test 
	public void testGroovy() {
		GeneralScriptFunction f = buildTestFunction("groovy","testGroovyUTF8.groovy");
		Output<JsonObject> output = run(f, "{\"key1\":\"val1\"}");
		Assert.assertEquals("kéÿ1",output.getPayload().getString("key1"));
	}
	
	@Test 
	public void testGroovyThrowable() {
		GeneralScriptFunction f = buildTestFunction("groovy","throwable.groovy");
		Output<JsonObject> output = run(f, "{}");
		assertTrue(output.getError().getMsg().contains("Error while running script throwable.groovy: assert false"));
	}
	
	@Test 
	public void testGroovyThrowableWithErrorHandler() {
		GeneralScriptFunction f = buildTestFunction("groovy","throwable.groovy");
		f.setErrorHandlerFile(new DynamicValue<String>(getScriptDir() + "/errorHandler.groovy"));
		Output<JsonObject> output = run(f, "{}");
		Assert.assertEquals("Error handler called",output.getError().getMsg());
	}
	
	
//	@Test 
//	public void testPython1() {
//		GeneralScriptFunction f = buildTestFunction("python","testPython.py");
//		Output<JsonObject> output = run(f, "{\"key1\":\"val1\"}");
//		Assert.assertEquals("val1",output.getResult().getString("key1"));
//	}
	
	@Test 
	public void testErrorWithoutErrorHandler() {
		GeneralScriptFunction f = buildTestFunction("javascript","errorScript.js");
		Output<JsonObject> out = run(f, "{}");
		assertTrue(out.getError().getMsg().contains("INVALID SCRIPT"));
		assertTrue(out.getError().getMsg().startsWith("Error while running script errorScript.js"));
		Assert.assertEquals(1,out.getAttachments().size());
	}
	
	@Test 
	public void testErrorHandler() {
		GeneralScriptFunction f = buildTestFunction("javascript","errorScript.js");
		f.setErrorHandlerFile(new DynamicValue<String>(getScriptDir() + "/errorHandler.js"));
		Output<JsonObject> out = run(f, "{}");
		assertTrue(out.getError().getMsg().contains("INVALID SCRIPT"));
	}
	
	@Test 
	// Test that attachments generated in the script are conserved after an exception is thrown
	public void testErrorScriptWithAttachmentAndWithoutErrorHandler() {
		GeneralScriptFunction f = buildTestFunction("javascript","errorScriptWithAttachment.groovy");
		Output<JsonObject> out = run(f, "{}");
		assertTrue(out.getError().getMsg().contains("INVALID"));
		assertTrue(out.getError().getMsg().startsWith("Error while running script"));
		Assert.assertEquals(2,out.getAttachments().size());
	}
	
	@Test 
	public void testErrorHandlerWithError() {
		GeneralScriptFunction f = buildTestFunction("javascript","errorScript.js");
		f.setErrorHandlerFile(new DynamicValue<String>(getScriptDir() + "/errorScript.js"));
		Output<JsonObject> out = run(f, "{}");
		assertTrue(out.getError().getMsg().contains("INVALID SCRIPT"));
		assertTrue(out.getError().getMsg().startsWith("Error while running error handler script:"));
		Assert.assertEquals(1,out.getAttachments().size());
	}

	@Test
	@Category(PerformanceTest.class)
	public void testParallel() throws InterruptedException, ExecutionException, TimeoutException {
		int nIt = 100;
		int nThreads = 10;
		ExecutorService e = Executors.newFixedThreadPool(nThreads);

		List<Future<Boolean>> results = new ArrayList<>();

		for(int i=0;i<nIt;i++) {
			results.add(e.submit(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					GeneralScriptFunction f = buildTestFunction("javascript","test1.js");
					try {
						//the FunctionRunner recreate a full context with underlying local token, application context builder...
						//so grid jar are registered in parallel which causes concurrent issues on the ResourceExtractor which has been synchronized now.
						Output<JsonObject> output = run(f, "{\"key1\":\"val1\"}");
						if (output.getError() != null) {
							logger.error(output.getError().getMsg());
							if (output.getAttachments() != null) {
								for (Attachment attachment : output.getAttachments()) {
									byte[] bytes = AttachmentHelper.hexStringToByteArray(attachment.getHexContent());
									logger.error("Exception: {}", new String(bytes, StandardCharsets.UTF_8));
								}
							}
						}
						Assert.assertEquals("val1", output.getPayload().getString("key1"));
						return true;
					} catch (Throwable e) {
						logger.error("Exception while running parallel thread.", e);
						return false;
					}
				}
			}));
		}

		boolean allResult = true;
		for (Future<Boolean> future : results) {
			try {
				allResult = allResult && future.get(1, TimeUnit.MINUTES);
			} catch (Throwable e1) {
				logger.error("Future throw an error.", e1);
			}
		}
		assertTrue(allResult);
	}
	
	private GeneralScriptFunction buildTestFunction(String scriptLanguage, String scriptFile) {
		GeneralScriptFunction f = new GeneralScriptFunction();
		f.setScriptLanguage(new DynamicValue<String>(scriptLanguage));
		f.setLibrariesFile(new DynamicValue<>());
		f.setId(new ObjectId());
		Map<String, String> attributes = new HashMap<>();
		attributes.put(AbstractOrganizableObject.NAME, "medor");
		f.setAttributes(attributes);

		f.setScriptFile(new DynamicValue<String>(getScriptDir() + "/" + scriptFile));
		return f;
	}
	
	private String getScriptDir() {
		return FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(),"scripts").getAbsolutePath();
	}
}
