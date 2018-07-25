package step.handlers.javahandler;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.grid.io.OutputMessage;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class KeywordRunnerTest {

	@Test
	public void test() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		OutputMessage output = runner.run("MyKeyword");
		Assert.assertEquals("test",output.getPayload().getString("test"));
	}
	
	@Test
	public void testProperties() throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put("prop1", "My Property");
		ExecutionContext runner = KeywordRunner.getExecutionContext(properties, MyKeywordLibrary.class);
		OutputMessage output = runner.run("MyKeywordUsingProperties");
		Assert.assertEquals("My Property",output.getPayload().getString("prop"));
	}
	
	@Test
	public void testSession() throws Exception {
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		OutputMessage output = runner.run("MyKeywordUsingSession1");
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
			OutputMessage output = runner.run("MyErrorKeyword");
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("My Error",exception.getMessage());
	}
	
	@Test
	public void testErrorKeywordWithThrowable() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		try {
			OutputMessage output = runner.run("MyErrorKeywordWithThrowable");
		} catch(Exception e) {
			exception = e;
		}
		// In this case the throwable thrown in the keyword is attached as Cause of the exception
		// The reason is that the current API of AbstractMessageHandler doesn't throws Throwable but Exception
		Assert.assertEquals("My throwable",exception.getCause().getMessage());
	}
	
	@Test
	public void testKeywordNotExisting() throws Exception {
		Exception exception = null;
		ExecutionContext runner = KeywordRunner.getExecutionContext(MyKeywordLibrary.class);
		try {
			OutputMessage output = runner.run("UnexistingKeyword");
			
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
			OutputMessage output = runner.run("MyKeyword");
			
		} catch(Exception e) {
			exception = e;
		}
		Assert.assertEquals("Unable to find method annoted by 'step.handlers.javahandler.Keyword' with name=='MyKeyword'",exception.getMessage());
	}
}
