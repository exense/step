package step.functions.handler;

import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import step.functions.io.Input;
import step.functions.io.Output;

public class TestFunctionHandler extends AbstractFunctionHandler<TestInput, TestOutput> {

	@Override
	protected Output<TestOutput> handle(Input<TestInput> input) throws Exception {
		HashMap<String, String> dummyInputProperties = new HashMap<String, String>();
		dummyInputProperties.put("testFile.id", FunctionMessageHandlerTest.EMPTY_FILE);
		dummyInputProperties.put("testFile.version", "1");
		
		// Test application context methods
		pushRemoteApplicationContext(FORKED_BRANCH, "testFile", dummyInputProperties);
		pushLocalApplicationContext(FORKED_BRANCH, this.getClass().getClassLoader(), "testResource.jar");
		Assert.assertTrue(((URLClassLoader)getCurrentContext(FORKED_BRANCH).getClassLoader()).getURLs()[0].getFile().contains("testResource.jar"));

		pushRemoteApplicationContext("testFile", dummyInputProperties);
		pushLocalApplicationContext(this.getClass().getClassLoader(), "testResource.jar");
		Assert.assertTrue(((URLClassLoader)getCurrentContext().getClassLoader()).getURLs()[0].getFile().contains("testResource.jar"));
		
		//  Test property merging
		Map<String, String> mergedProperties = mergeAllProperties(input);
		Assert.assertEquals("myTokenPropValue1", mergedProperties.get("myTokenProp1"));
		Assert.assertEquals("myAgentPropValue1", mergedProperties.get("myAgentProp1"));
		Assert.assertEquals("myInputPropValue1", mergedProperties.get("myInputProp1"));
		
		// Test payload
		Assert.assertEquals("Hallo", input.getPayload().getMessage());
		
		//runInContext(callable)
		
		// Test getSessions methods
		Assert.assertNotNull(getTokenSession());
		Assert.assertNull(getTokenReservationSession());
		
		Assert.assertNotNull(getProperties());

		// Test delegation
		return delegate(SecondTestFunctionHandler.class.getName(), input);
	}

	@Override
	public Class<TestInput> getInputPayloadClass() {
		return TestInput.class;
	}

	@Override
	public Class<TestOutput> getOutputPayloadClass() {
		return TestOutput.class;
	}

}
