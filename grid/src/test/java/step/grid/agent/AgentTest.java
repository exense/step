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
package step.grid.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.grid.client.GridClient.TokenHandle;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class AgentTest extends AbstractGridTest {
	
	@Test
	public void test() throws Exception {
		Map<String, Interest> interests = new HashMap<>();
		interests.put("att1", new Interest(Pattern.compile("val.*"), true));
		
		JsonObject o = newDummyJson();
		
		OutputMessage outputMessage = client.getToken(null, interests).setHandler("class:step.grid.agent.TestTokenHandler").processAndRelease("testFunction", o);
		Assert.assertEquals(outputMessage.getPayload(), o);;
	}
	
	@Test
	public void testTimeout() throws Exception {		
		JsonObject o = Json.createObjectBuilder().add("delay", 4000).build();
		
		TokenHandle f = client.getToken().setCallTimeout(10).setHandler("class:step.grid.agent.TestTokenHandler");
		OutputMessage outputMessage = f.processAndRelease("testFunction", o);
		Assert.assertEquals("Timeout while processing request. Request execution interrupted successfully.",outputMessage.getError());
		Assert.assertTrue(outputMessage.getAttachments().get(0).getName().equals("stacktrace.log"));
		
		
		// check if the token has been returned to the pool. In this case the second call should return the same error
		outputMessage = f.processAndRelease("testFunction", o);
		Assert.assertEquals("Timeout while processing request. Request execution interrupted successfully.",outputMessage.getError());
	}
	
	@Test
	public void testTimeoutNoTokenReturn() throws Exception {		
		JsonObject o = Json.createObjectBuilder().add("delay", 4000).add("delayAfterInterruption", 150).build();
		
		TokenHandle f = client.getToken().setCallTimeout(10).setHandler("class:step.grid.agent.TestTokenHandler");
		OutputMessage outputMessage = f.processAndRelease("testFunction", o);
		Assert.assertEquals("Timeout while processing request. WARNING: Request execution couldn't be interrupted and the token couldn't be returned to the pool. Subsequent calls to that token may fail!",outputMessage.getError());
		Assert.assertTrue(outputMessage.getAttachments().get(0).getName().equals("stacktrace.log"));

		
		// check if the token has been returned to the pool. In this case the second call should return the same error
		outputMessage = f.processAndRelease("testFunction", o);
		Assert.assertTrue(outputMessage.getError().contains("already in use"));
		
		Thread.sleep(150);
		o = newDummyJson();
		
		// After "delayAfterInterruption" the token should have been returned to the pool
		outputMessage = f.processAndRelease("testFunction", o);
		Assert.assertEquals(outputMessage.getPayload(), o);;
	}
	
	
	
	
	@Test
	public void testDefaultHandler() throws Exception {
		Map<String, Interest> interests = new HashMap<>();
		interests.put("att1", new Interest(Pattern.compile("val2"), true));
		
		Map<String, String> attributes = new HashMap<>();
		attributes.put("att1", "val2");
		
		Map<String, String> properties = new HashMap<>();
		properties.put("tokenhandler.default", "class:step.grid.agent.TestTokenHandler");
		
		addToken(1, attributes, properties);
		
		JsonObject o = newDummyJson();
		
		OutputMessage outputMessage = client.getToken(null, interests).processAndRelease("testFunction", o);
		Assert.assertEquals(outputMessage.getPayload(), o);;
	}
	
	@Test
	public void testHandlerChain() throws Exception {		
		final String uri = "class:step.grid.agent.TestTokenHandlerDelegator|class:step.grid.agent.TestTokenHandler";
		JsonObject o = newDummyJson();
		OutputMessage outputMessage = client.getToken().setHandler(uri).processAndRelease("function1",  o);
		Assert.assertEquals(o, outputMessage.getPayload());
		Assert.assertEquals("Test error", outputMessage.getError());
	}
	
	@Test
	public void testLocalToken() throws Exception {
		JsonObject o = newDummyJson();
		
		OutputMessage outputMessage = client.getLocalToken().setHandler("class:step.grid.agent.TestTokenHandler").processAndRelease("testFunction", o);
		Assert.assertEquals(outputMessage.getPayload(), o);;
	}
}
