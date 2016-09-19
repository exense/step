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

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class AgentTest extends AbstractGridTest {
	
	@Test
	public void test() throws Exception {
		Map<String, Interest> interests = new HashMap<>();
		interests.put("att1", new Interest(Pattern.compile("val.*"), true));
		
		JsonObject o = newDummyJson();
		
		OutputMessage outputMessage = client.getToken(null, interests).processAndRelease("testFunction", o, "class:step.grid.agent.TestTokenHandler", null);
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
		
		OutputMessage outputMessage = client.getToken(null, interests).processAndRelease("testFunction", o, null);
		Assert.assertEquals(outputMessage.getPayload(), o);;
	}
	
	@Test
	public void testHandlerChain() throws Exception {		
		final String uri = "class:step.grid.agent.TestTokenHandlerDelegator|class:step.grid.agent.TestTokenHandler";
		JsonObject o = newDummyJson();
		OutputMessage outputMessage = client.getToken().processAndRelease("function1",  o, uri, null);
		Assert.assertEquals(o, outputMessage.getPayload());
		Assert.assertEquals("Test error", outputMessage.getError());
	}
}
