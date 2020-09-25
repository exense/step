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
