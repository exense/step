package step.handlers.javahandler;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class KeywordRunnerTest {

	@Test
	public void test() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeyword");
		Assert.assertEquals("test",output.getPayload().getString("test"));
	}
	
	@Test
	public void testProperties() throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put("prop1", "My Property");
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeywordUsingProperties");
		Assert.assertEquals("My Property",output.getPayload().getString("prop"));
	}
	
	@Test
	public void testSession() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		Output<JsonObject> output = runner.run("MyKeywordUsingSession1");
		Assert.assertEquals("test", System.getProperty("testProperty"));
		output = runner.run("MyKeywordUsingSession2");
		Assert.assertEquals("Test String",output.getPayload().getString("sessionObject"));
		runner.close();
		// Asserts that the close method of the session object created in MyKeywordUsingSession2 has been called
		Assert.assertNull(System.getProperty("testProperty"));
	}
	
	@Test
	public void testError() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		try {
			runner.run("MyErrorKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("My error",exception.getMessage());
	}
	
	@Test
	public void testException() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		try {
			runner.run("MyExceptionKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("My exception",exception.getMessage());
		Assert.assertTrue(exception instanceof KeywordException);
		// the exception thrown by the keyword is attached as cause
		Assert.assertNotNull(exception.getCause());
	}

	@Test
	public void testErrorKeywordWithThrowable() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		try {
			runner.run("MyErrorKeywordWithThrowable");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("My throwable",exception.getMessage());
	}
	
	@Test
	public void testRunnerDoesntThrowExceptionOnError() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		// we're testing here the following flag. In that case no exception should be thrown in case of an error
		runner.setThrowExceptionOnError(false);
		
		Output<JsonObject> output = null;
		try {
			output = runner.run("MyErrorKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertNotNull(output);
		Assert.assertEquals("My error",output.getError().getMsg());
		Assert.assertNull(exception);
	}
	
	@Test
	public void testRunnerDoesntThrowExceptionOnException() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		// we're testing here the following flag. In that case exceptions thrown in the keyword should be reported as error
		runner.setThrowExceptionOnError(false);
		
		Output<JsonObject> output = null;
		try {
			output = runner.run("MyExceptionKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertNotNull(output);
		Assert.assertEquals("My exception",output.getError().getMsg());
		Assert.assertNull(exception);
	}
	
	@Test
	public void testKeywordNotExisting() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		try {
			runner.run("UnexistingKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("Unable to find method annoted by 'step.handlers.javahandler.Keyword' with name=='UnexistingKeyword'",exception.getMessage());
	}
	
	@Test
	public void testEmptyLibraryList() throws Exception {
		Exception exception = null;
		try {
			KeywordRunner.getExecutionContext();
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("Please specify at leat one class containing the keyword definitions",exception.getMessage());
	}
	
	@Test
	public void testEmptyKeywordLibrary() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyEmptyKeywordLibrary.class);
		try {
			runner.run("MyKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("Unable to find method annoted by 'step.handlers.javahandler.Keyword' with name=='MyKeyword'",exception.getMessage());
	}
}
