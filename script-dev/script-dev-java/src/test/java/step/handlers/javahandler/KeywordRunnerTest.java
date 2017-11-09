package step.handlers.javahandler;

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
}
